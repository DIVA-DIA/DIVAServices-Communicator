package request;

import com.google.gson.JsonElement;
import diva.DivaServicesConnection;
import diva.HttpRequest;
import diva.ImageEncoding;
import exceptions.CollectionException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import jdk.nashorn.internal.parser.JSONParser;

/**
 * @author Marcel Würsch
 *         marcel.wuersch@unifr.ch
 *         http://diuf.unifr.ch/main/diva/home/people/marcel-w%C3%BCrsch
 *         Created on: 18.10.2016.
 */
public class DivaCollection {
    private String name;

    private DivaCollection(String name){
        this.name = name;
    }


    public static DivaCollection createCollectionWithImages(List<BufferedImage> images, DivaServicesConnection connection) throws MalformedURLException, IOException{
        JSONObject request = new JSONObject();
        JSONArray jsonImages = new JSONArray();
        for (BufferedImage image : images){
            JSONObject jsonImage = new JSONObject();
            jsonImage.put("type","image");
            jsonImage.put("value", ImageEncoding.encodeToBase64(image));
            jsonImages.put(jsonImage);
        }
        request.put("images",jsonImages);
        JSONObject response = HttpRequest.executePost(connection.getServerUrl() + "/upload", request);
        
        String collection = response.getString("collection");
        String url = connection.getServerUrl() + "/collections/" + collection;
        JSONObject getResponse = HttpRequest.executeGet(url);
        
        while(!(getResponse.getInt("percentage") == 100)){
            try {
                Thread.sleep(connection.getCheckInterval() * 1000);
                getResponse = HttpRequest.executeGet(url);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return new DivaCollection(collection);
    }

    public static DivaCollection createCollectionByName(String name, DivaServicesConnection connection) throws CollectionException, MalformedURLException, IOException {
        JSONObject response = HttpRequest.executeGet(connection.getServerUrl() + "/collections/" + name);
        if(response.getInt("statusCode") == 200){
            return new DivaCollection(name);
        }else{
            throw new CollectionException("Collection: " + name + " does not exists on the remote system");
        }
    }
    public String getName(){
        return name;
    }
}
