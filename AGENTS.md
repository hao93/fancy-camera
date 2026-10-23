# Fancy Camera 协作约定

- 中文回复，保留标准包名com.silverroom.camera和原签名，覆盖升级保留底片。
- 用户已确认使用公开仓库hao93/fancy-camera：新版完成后，默认推送代码，打v版本标签并发布带标准APK、update.json和校验值的GitHub Release，不重复询问是否发布。
- 标准构建默认测试、发布Release并尝试安装到本地已配对手机；只构建使用-SkipPublish -SkipInstall。
- 只向相邻adb-state/device.json配置中序列号和型号完全匹配的手机执行adb install -r，不卸载或清除数据。地址变化先核查连接，不默认重新配对。
- 构建成功、Release发布成功与手机安装成功分别核实并报告，不能混为一谈。
- 签名私钥、签名密码、GitHub令牌和ADB配对凭据仅在本地保存，不提交、不加入源码包、不打入APK；GitHub公开证书指纹可以提交。
- publish.ps1采用文件白名单提交、草稿上传校验后公开Release，不覆盖已发布版本，不强推。
- 更新检查只在App前台打开和手动检查时执行，无后台定时任务；无新版或检查失败静默，不触碰拍摄流程。
