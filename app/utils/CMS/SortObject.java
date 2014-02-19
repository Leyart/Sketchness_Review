package utils.CMS;

/**
 * Created with IntelliJ IDEA.
 * User: Riboni1989
 * Date: 12/11/13
 * Time: 12:49
 * To change this template use File | Settings | File Templates.
 */
public abstract class SortObject implements Comparable<SortObject> {

    private String id;
    private int idU;
    private int num;
    private String media;

    private int idTmp;
    private int imgTmp;

    public void setId(String id){
        this.id = id;
    }
    public void setNum(int num){
        this.num = num;
    }
    public void setIdU(int idU){
        this.idU = idU;
    }
    public void setIdTmp(int idTmp){
        this.idTmp = idTmp;
    }
    public void setImgTmp(int imgTmp){
        this.imgTmp = imgTmp;
    }
    public void setMedia(String media){
        this.media = media;
    }
    public String getId(){
        return id;
    }
    public int getNum(){
        return num;
    }
    public int getIdU(){
        return idU;
    }
    public int getIdTmp(){
        return idTmp;
    }
    public int getImgTmp(){
        return imgTmp;
    }
    public String getMedia(){
        return media;
    }

    @Override
    public int compareTo(SortObject o) {
        String i = Integer.toString(getNum());
        String j = Integer.toString(o.getNum());
        return i.compareTo(j);
    }


    public int compare(){
        return 1;
    }




}
