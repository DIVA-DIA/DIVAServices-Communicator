package diva;

import exceptions.MethodNotAvailableException;
import org.json.JSONArray;
import org.json.JSONObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import static com.sun.corba.se.spi.presentation.rmi.StubAdapter.request;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import jdk.nashorn.internal.parser.JSONParser;
import org.apache.http.ParseException;
import returnTypes.DivaServicesResponse;
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author 317617032205
 */
public class DivaServicesAdmin{
    
    //Test
    
    public static void main(String[] args) throws MethodNotAvailableException, IOException {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("sharpenLevel", 8); 
        parameters.put("inputImage", "lightcoralpalefrogmouth/bnf-lat11641_001r.jpeg");
        List<JSONObject> result = runMethod("http://divaservices.unifr.ch/api/v2/enhancement/sharpenenhancement/1", parameters);
        
     //   List<JSONObject> result = runMethod ("http://divaservices.unifr.ch/api/v2/graph/graphextraction/1", parameters);
        if(result != null){ 
            System.out.println("finalresult: "+result);
        }else{
            // exception
        }   
            
    }
                    //return list of JSONObject
    public static List<JSONObject> runMethod(String url, Map<String, Object> parameters) throws MethodNotAvailableException, IOException{
        JSONObject postRequest= checkParams(runGetRequest(url), parameters);
        return runPostRequest(url, postRequest);
        
        // No exceptions. The parameters are OK
        //System.out.println("PARAMS ARE OKOKOKOKOK");
    }
   
    
    private static List<JSONObject> runPostRequest(String url, JSONObject postRequest) throws IOException{
        List<JSONObject> results = null;
        JSONObject postResult = HttpRequest.executePost(url, postRequest);
        //TODO: Check if postResult contains no error
        // 202 correc, if 500 false
          System.out.println("postresult:  " +postResult);
        
        // processDivaRequest(request, postRequest);
        if(postResult.has("statusCode") && postResult.getInt("statusCode")==202){   //correct
            results = HttpRequest.getResult(postResult, 5); //checkinterval How often?
        }else if(postResult.has("statusCode") && postResult.getInt("statusCode")!=202){  //wrong
            // exception
        }else{
            // exception
        }
        return results;
        /**
         * Task 3 Poll for result
         */
     //   logJsonObject(postObject);
    }
    
    private static JSONObject runGetRequest(String url) throws MethodNotAvailableException, MalformedURLException, IOException {
        JSONObject response = HttpRequest.executeGet(url);        
        if((response.has("status") && response.getInt("status") == 404)){
            throw new MethodNotAvailableException("This method is currently unavailable");
        }
        return response;
    }
    
    private static JSONObject checkParams(JSONObject object, Map<String, Object> userParams) {
        JSONObject postRequest = new JSONObject();
        JSONObject parameters = new JSONObject();
        JSONArray dataJA = new JSONArray();
        
        JSONArray arrayOfInputs = object.getJSONArray("input");
      
        int countmatch = 0;
        for(int j = 0 ; j < arrayOfInputs.length() ; j++){
            String input = arrayOfInputs.getJSONObject(j).toString();
            int i = 1;
            // Because the inputtype name, i.e. file/number/select etc., could change, we will not access its content by its name
            while(input.charAt(i)!='{'){
                ++i;                                                    
            }
            String inputTypeName = input.substring(2, i-2);
            String inputTypeContentS = input.substring(i, input.length()-1);  
            JSONObject inputTypeContent = new JSONObject(inputTypeContentS);
            if(inputTypeContent.has("userdefined") && inputTypeContent.getBoolean("userdefined")== true ){
                String parameter = inputTypeContent.getString("name"); 
                if(!userParams.containsKey(parameter)){     //********* If forgot <key,vale> object, still use default values? 
                    // client exception "you forgot a <key,value> object"
                }
                Object userValue = userParams.get(parameter);
                JSONObject options = inputTypeContent.getJSONObject("options"); 
                if(inputTypeName.equals("select")){        
                    String values = options.getJSONArray("values").toString();
                    if(userValue != null && values.contains(userValue.toString())){
                        parameters.put(parameter, userValue.toString());        //if a select-userValue is a number, should it be put as number or string?
                        ++countmatch;
                    }else if(userValue == null){                
                        int p = options.getInt("default");
                        String defaultValue = options.getJSONArray("values").getString(p);
                        parameters.put(parameter, defaultValue);
                        ++countmatch;
                    }else{
                            // client exception "not accepted value for this parameter"
                    }
                }else if(inputTypeName.equals("number")){
                    double min = options.getDouble("min");   
                    double max = options.getDouble("max");
                    double steps = options.getDouble("steps");
                    double k=min;
                    double userValueD;
                    if(userValue != null){
                        userValueD = new Double(userValue.toString());
                        for(; k<=max; k=k+steps){
                            if(userValueD == k){
                                break;
                            }
                        }
                        if(k<=max){
                            parameters.put(parameter, userValueD);
                            ++countmatch;
                        }else{
                            //client exception "not accepted value for this parameter"
                        }
                    }else{   // if userValue == null
                        double defaultValue = options.getDouble("default");
                        parameters.put(parameter, defaultValue);
                        ++countmatch;
                    }
                          
                }else if(inputTypeName.equals("file")){
                    String mimeType = options.getString("mimeType").split("/")[1]; // jpeg, jpg etc. (without image/...)
                    if(userValue != null){
                        String userMimeType = userValue.toString().split("\\.")[1];
                        if(userMimeType.equals(mimeType)){
                            JSONObject dataObject = new JSONObject();
                            dataObject.put(parameter, userValue.toString());
                            dataJA.put(dataObject);
                            ++countmatch;
                        }else{
                            //client exception "not accepted value for this parameter"
                        }
                    }else{  //if userValue == null
                            // client excpetion "value required for this parameter" (there is no default)
                    }
                }else{
                            //server exception "wrong filetype"
                }
                // else if (inputTypeName.equals("highlighter"))
            }
        } 
        
        if(countmatch < userParams.size()){
            //client exception "you put too many parameters. you need to insert values for:..."
        }
        // ITS ALL OK
        postRequest.put("data", dataJA);
        postRequest.put("parameters", parameters);
        System.out.println("********************************");
        System.out.println("postrequest: "+postRequest);
        return postRequest;
    }    
    
    private static void logJsonObject(JSONObject object) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(object.toString());
        System.out.println(gson.toJson(je));
    }
    
    
}
