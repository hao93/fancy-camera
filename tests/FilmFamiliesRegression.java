import com.silverroom.camera.*;
import java.util.*;

public final class FilmFamiliesRegression {
    static int count;
    static void check(boolean value,String label){if(!value)throw new AssertionError(label);count++;System.out.println("PASS "+label);}
    static float[] recipe(int id){return FilmCatalog.parameters(id,0,0,0,0,0,0);}
    public static void main(String[] args){
        check(FilmCatalog.NAMES.length==51,"New recipes append after the stable existing twenty-four");
        String[] legacy={"原生负片","暖金 400","青橙电影","樱桃 200","银盐黑白","午夜 800","经典铬彩","经典负片","怀旧负片","自然真实","标准正片","鲜艳反转","柔和人像","影院柔彩","漂白电影","细腻黑白","柔和负片","暖调棕片"};
        check(Arrays.equals(Arrays.copyOf(FilmCatalog.NAMES,18),legacy),"All historical negative IDs retain their names and order");
        check(FilmGroups.ids(1).equals(Arrays.asList(6,7,8,9,10,11,12,13,14,15,16,17)),"Fuji never absorbs newly appended camera families");
        check(FilmGroups.ids(2).equals(Arrays.asList(18,19,20)),"Leica reel contains exactly its three new recipes");
        check(FilmGroups.ids(3).equals(Arrays.asList(21,22,23)),"Ricoh reel contains exactly its three new recipes");
        check(FilmGroups.ids(4).equals(Arrays.asList(0,1,2,3,4,5)),"Original family keeps the six existing custom recipes");
        check(FilmGroups.ids(0).size()==51,"All-film reel still includes every old and new recipe");
        Set<Integer> union=new HashSet<>();int sum=0;for(int g=1;g<FilmGroups.LABELS.length;g++){List<Integer> ids=FilmGroups.ids(g);union.addAll(ids);sum+=ids.size();}
        check(union.size()==51&&sum==51,"Named families partition the catalog without overlaps or gaps");
        check(FilmGroups.choose(2,7,19)==19,"Switching families restores a valid remembered recipe");
        check(FilmGroups.choose(2,20,19)==20,"Reopening the active family preserves its selected recipe");
        check(FilmGroups.choose(3,19,19)==21,"A stale remembered recipe cannot leak into the wrong family");
        check(FilmGroups.choose(0,23,-1)==23,"Returning to All keeps the currently applied recipe");
        check(FilmGroups.safe(-1)==0&&FilmGroups.safe(100)==0&&!FilmGroups.contains(0,-1),"Corrupt group preferences safely recover without invalid recipes");
        boolean aliases=true;for(int i=18;i<=20;i++)aliases&=FilterShelf.matches(i," LEICA ")&&FilterShelf.matches(i,"徕卡");for(int i=21;i<=23;i++)aliases&=FilterShelf.matches(i,"RICOH")&&FilterShelf.matches(i,"理光");
        check(aliases,"Every new style is searchable by its Chinese and English family name");
        check(!FilterShelf.matches(18,"ricoh")&&!FilterShelf.matches(21,"fuji"),"Brand search never matches the wrong family");
        check(FilterShelf.parse("23,20,21,18",true).equals(Arrays.asList(23,20,21,18)),"New recipe favorites survive preference encoding");
        check(recipe(20)[6]==0&&recipe(23)[6]==0&&recipe(23)[7]>recipe(20)[7],"New monochrome recipes stay neutral with distinct contrast");
        check(recipe(19)[6]>recipe(18)[6]&&recipe(21)[6]>recipe(22)[6],"Vivid and positive recipes differ from their restrained counterparts");
        List<Integer> all=FilmGroups.ids(0);FilmWheelState wheel=new FilmWheelState();wheel.reset(FilmGroups.ids(3),23);Set<Integer> reached=new HashSet<>();for(int i=0;i<wheel.count();i++){wheel.position(i);reached.add(wheel.selected());}
        check(reached.equals(new HashSet<>(Arrays.asList(21,22,23))),"Grouped reel reaches every Ricoh style without adding foreign IDs");
        int[] ids=new int[all.size()];for(int i=0;i<ids.length;i++)ids[i]=all.get(i);int columns=PreviewAtlas.columns(224,224,ids.length),height=224*((ids.length+columns-1)/columns);PreviewAtlas atlas=new PreviewAtlas(224,224,ids,new byte[224*columns*height*4],0);
        check(atlas.height<=2048&&atlas.width<=2048&&atlas.index(50)==50&&atlas.pixels(50).length==224*224,"Full 51-recipe atlas stays bounded and decodes the last tile");
        System.out.println("ALL "+count+" FILM FAMILY TESTS PASSED");
    }
}
