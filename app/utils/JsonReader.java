package utils;

import org.codehaus.jackson.JsonNode;
import play.libs.F.Promise;
import play.libs.WS;
import play.libs.WS.Response;

public class JsonReader {

    /*
     * Method that returns a Json object from a request sent to the CMS
     * @param url Url of the API of the CMS to be called
     */
    public JsonNode readJsonArrayFromUrl(String url) {
        //May be improved with http://stackoverflow.com/questions/15453905/promise-timeouts-and-ws-get-timeout-in-playframework-2-1-java
        Promise<WS.Response> res;
        try {
            res = WS.url(url).setTimeout(1000000).get();
            if (res != null) {
                Response result = res.get(1000000L);
                JsonNode json = result.asJson();
                //with a system out i can see that the json is parsed correctly
                return json;
            } else {
                throw new IllegalStateException("CMS response timeout.");
            }
        } catch (Exception mf) {
            throw new IllegalArgumentException("The URL that has been provided is not valid");
        }
    }
}
