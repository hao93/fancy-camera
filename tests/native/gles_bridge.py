"""Optional desktop EGL pixel checks. Uses the supplied SDK libraries, never a phone."""
import ctypes as c
import json
import os
from pathlib import Path
import sys

folder = Path(sys.argv[1]).resolve()
dll_path = os.add_dll_directory(str(folder)) if hasattr(os, "add_dll_directory") else None
egl = c.CDLL(str(folder / "libEGL.dll"))
gl = c.CDLL(str(folder / "libGLESv2.dll"))
I, U, F, P = c.c_int, c.c_uint, c.c_float, c.c_void_p


def function(lib, name, result, args):
    f = getattr(lib, name)
    f.restype, f.argtypes = result, args
    return f


get_display = function(egl, "eglGetDisplay", P, [P])
display = get_display(None)
major, minor = I(), I()
if not function(egl, "eglInitialize", I, [P, c.POINTER(I), c.POINTER(I)])(display, c.byref(major), c.byref(minor)):
    raise RuntimeError("EGL initialization failed")
attributes = (I * 13)(0x3033, 1, 0x3040, 4, 0x3024, 8, 0x3023, 8, 0x3022, 8, 0x3021, 0, 0x3038)
configs, count = (P * 128)(), I()
if not function(egl, "eglChooseConfig", I, [P, c.POINTER(I), c.POINTER(P), I, c.POINTER(I)])(display, attributes, configs, 128, c.byref(count)) or not count.value:
    raise RuntimeError("No EGL ES2 pbuffer configuration")
attribute = function(egl, "eglGetConfigAttrib", I, [P, P, I, c.POINTER(I)])
chosen = configs[0]
for n in range(count.value):
    alpha = I()
    attribute(display, configs[n], 0x3021, c.byref(alpha))
    if alpha.value == 0:
        chosen = configs[n]
        break
function(egl, "eglBindAPI", I, [I])(0x30A0)
surface = function(egl, "eglCreatePbufferSurface", P, [P, P, c.POINTER(I)])(display, chosen, (I * 5)(0x3057, 64, 0x3056, 64, 0x3038))
context = function(egl, "eglCreateContext", P, [P, P, P, c.POINTER(I)])(display, chosen, None, (I * 3)(0x3098, 2, 0x3038))
if not context or not surface or not function(egl, "eglMakeCurrent", I, [P, P, P, P])(display, surface, surface, context):
    raise RuntimeError("EGL make-current failed")
signatures = {
    "glActiveTexture": (None, [U]), "glBindTexture": (None, [U, U]),
    "glTexParameteri": (None, [U, U, I]), "glTexImage2D": (None, [U, I, I, I, I, I, U, U, P]),
    "glCopyTexSubImage2D": (None, [U, I, I, I, I, I, I, I]),
    "glBindFramebuffer": (None, [U, U]), "glFramebufferTexture2D": (None, [U, U, U, U, I]),
    "glCheckFramebufferStatus": (U, [U]), "glGetError": (U, []),
    "glCreateShader": (U, [U]), "glCompileShader": (None, [U]), "glDeleteShader": (None, [U]),
    "glCreateProgram": (U, []), "glAttachShader": (None, [U, U]), "glLinkProgram": (None, [U]),
    "glDeleteProgram": (None, [U]), "glUseProgram": (None, [U]),
    "glUniform1i": (None, [I, I]), "glUniform1f": (None, [I, F]),
    "glGetUniformLocation": (I, [U, c.c_char_p]), "glGetAttribLocation": (I, [U, c.c_char_p]),
    "glEnableVertexAttribArray": (None, [U]), "glDrawArrays": (None, [U, I, I]),
    "glEnable": (None, [U]), "glDisable": (None, [U]), "glBlendFunc": (None, [U, U]),
    "glViewport": (None, [I, I, I, I]), "glClearColor": (None, [F, F, F, F]), "glClear": (None, [U]),
}
functions = {name: function(gl, name, result, args) for name, (result, args) in signatures.items()}
vertex_arrays = {}


def dispatch(name, args):
    if name in ("glGenTextures", "glGenFramebuffers"):
        values = (U * args[0])()
        function(gl, name, None, [I, c.POINTER(U)])(args[0], values)
        return list(values)
    if name == "glGetIntegerv":
        values = (I * (4 if args[0] == 0x0BA2 else 1))()
        function(gl, name, None, [U, c.POINTER(I)])(args[0], values)
        return list(values)
    if name in ("glGetShaderiv", "glGetProgramiv"):
        value = I()
        function(gl, name, None, [U, U, c.POINTER(I)])(*args, c.byref(value))
        return value.value
    if name == "glShaderSource":
        data = c.c_char_p(args[1].encode())
        function(gl, name, None, [U, I, c.POINTER(c.c_char_p), P])(args[0], 1, c.byref(data), None)
        return None
    if name in ("glGetUniformLocation", "glGetAttribLocation"):
        return functions[name](args[0], args[1].encode())
    if name == "glTexImage2D":
        return functions[name](*args, None)
    if name == "glVertexAttribPointer":
        values = (F * len(args[5]))(*args[5])
        vertex_arrays[args[0]] = values
        function(gl, name, None, [U, I, U, c.c_ubyte, I, P])(*args[:5], c.cast(values, P))
        return None
    if name == "pixel":
        pixels = (c.c_ubyte * 4)()
        function(gl, "glReadPixels", None, [I, I, I, I, U, U, P])(args[0], args[1], 1, 1, 0x1908, 0x1401, pixels)
        return list(pixels)
    return functions[name](*args)


renderer = function(gl, "glGetString", c.c_char_p, [U])(0x1F01).decode()
alpha = I()
attribute(display, chosen, 0x3021, c.byref(alpha))
print(json.dumps({"renderer": renderer, "alpha": alpha.value}), flush=True)
try:
    for line in sys.stdin:
        request = json.loads(line)
        try:
            if request["op"] == "quit":
                print('{"v":true}', flush=True)
                break
            value = dispatch(request["op"], request["args"])
            print(json.dumps({"v": value}), flush=True)
        except Exception as exc:
            print(json.dumps({"error": str(exc)}), flush=True)
finally:
    function(egl, "eglMakeCurrent", I, [P, P, P, P])(display, None, None, None)
    function(egl, "eglDestroyContext", I, [P, P])(display, context)
    function(egl, "eglDestroySurface", I, [P, P])(display, surface)
    function(egl, "eglTerminate", I, [P])(display)
