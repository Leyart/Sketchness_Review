package utils.CMS;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Riboni1989
 * Date: 18/11/13
 * Time: 17:11
 * To change this template use File | Settings | File Templates.
 */
public class DownObject {


    private int id;
    private ArrayList<StoredStatObj> element = new ArrayList<>();

    public int getId() {
        return id;
    }

    public void setId(int userId) {
        this.id = userId;
    }

    public ArrayList<StoredStatObj> getElement() {
        return element;
    }

    public void setElement(ArrayList<StoredStatObj> element) {
        this.element = element;
    }


}
