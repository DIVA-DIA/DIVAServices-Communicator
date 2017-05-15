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
    public static void main(String[] args) throws MethodNotAvailableException, IOException {
        //TODO: Make this request work and write to the console all available methods
        //checkParams(runGetRequest("http://192.168.56.102:8080/enhancement/sharpenenhancement/1"));//json array
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("inputImage", 4); // user enters number as int or as string or both?
        DivaServicesResponse<JSONObject> result = runMethod("http://divaservices.unifr.ch/api/v2/graph/graphextraction/1", parameters);
        // http://divaservices.unifr.ch/api/v2/graph/graphextraction/1
    }
                    //return list of JSONObject
    public static DivaServicesResponse<JSONObject> runMethod(String url, Map<String, Object> parameters) throws MethodNotAvailableException, IOException{
        JSONObject postRequest= checkParams(runGetRequest(url), parameters);
        return runPostRequest(url, postRequest);
        
        // No exceptions. The parameters are OK
        //System.out.println("PARAMS ARE OKOKOKOKOK");
        
        
    }
   
    
    private static DivaServicesResponse<JSONObject> runPostRequest(String url, JSONObject postRequest) throws IOException{
      
        JSONObject postResult = HttpRequest.executePost(url, postRequest);
        //TODO: Check if postResult contains no error
        // 202 correc, if 500 false
        
        // processDivaRequest(request, postRequest);
        if(postResult.has("status") && postResult.getInt("status")==202){   //correct
            List<List<Map>> outputs = new LinkedList<>();
            for (int i = 0; i < postResult.getJSONArray("results").length(); i++) {
                List<JSONObject> results = HttpRequest.getResult(postResult, 5, request); //checkinterval How often?
                for (JSONObject result : results) {
                    List<Map> output = extractOutput(result.getJSONArray("output"));
                    outputs.add(output);
                }
            }
            return new DivaServicesResponse<>(null, outputs, null);

        }else if(postResult.has("status") && postResult.getInt("status")==500){  //wrong
            // exception
            
        }else{      // might be wrong too     
            // exception
        }
       
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
                if(!userParams.containsKey(parameter)){
                    // client exception "you forgot a <key,value> object"
                }
                JSONObject options = inputTypeContent.getJSONObject("options"); //******** obwohl required== true trotzdem default gebrauchen?
                if(options.has("required") && options.getBoolean("required")==true){
                    if(inputTypeName.equals("select")){
                        String userValue = userParams.get(parameter).toString();
                        String values = options.getJSONArray("values").toString();
                        if(values.contains(userValue)){
                            parameters.put(parameter, userValue);
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
                        double userValue = (double) userParams.get(parameter);
                        double min = options.getDouble("min");   
                        double max = options.getDouble("max");
                        double steps = options.getDouble("steps");
                        double k=min;
                        for(; k<=max; k=k+steps){
                            if(userValue == k){
                                break;
                            }
                        }
                        if(k<=max){
                            parameters.put(parameter, userValue);
                            ++countmatch;
                        }else if((Object) userValue == null){    //******** (Object) ok ??
                            double defaultValue = options.getDouble("default");
                            parameters.put(parameter, defaultValue);
                            ++countmatch;
                        }else{
                            //client exception "not accepted value for this parameter"
                        }
                          
                    }else if(inputTypeName.equals("file")){
                        String userValue = userParams.get(parameter).toString();
                        String mimeType = options.getString("mimeType").split("/")[1]; // jpeg etc. (without image/...)
                        if(userValue.split(".")[1].equals(mimeType)){
                            JSONObject dataObject = new JSONObject();
                            dataObject.put(parameter, userValue);
                            dataJA.put(dataObject);
                            ++countmatch;
                        }else if(userValue == null){
                            // client excpetion "value required for this parameter"
                        }else{
                            //client exception "not accepted value for this parameter" 
                        }
                    }else{
                            //server exception "wrong filetype"
                    }
                }else if(options.has("required") && options.getBoolean("required")==false){
                       
                    if(inputTypeName.equals("select")){ 
                        
                        String userValue = userParams.get(parameter).toString();
                        String values = options.getJSONArray("values").toString();
                        if(values.contains(userValue)){
                            parameters.put(parameter, userValue);
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
                        double userValue = (double) userParams.get(parameter);
                        double min = options.getDouble("min");   
                        double max = options.getDouble("max");
                        double steps = options.getDouble("steps");
                        double k=min;
                        for(; k<=max; k=k+steps){
                            if(userValue == k){
                                break;
                            }
                        }
                        if(k<=max){
                            parameters.put(parameter, userValue);
                            ++countmatch;
                        }else if((Object) userValue == null){    //******** (Object) ok ??
                            double defaultValue = options.getDouble("default");
                            parameters.put(parameter, defaultValue);
                            ++countmatch;
                        }else{
                            //client exception "not accepted value for this parameter"
                        }
/*                  }else if(inputTypeName.equals("file")){      //******* this case exists? userdefined==true and required==false?
                        String userValue = userParams.get(parameter).toString();
                        String mimeType = options.getString("mimeType").split("/")[1]; // jpeg etc. (without image/...)
                        if(userValue.split(".")[1].equals(mimeType)){
                            JSONObject dataJO = new JSONObject();
                            dataJO.put(parameter, userValue);
                            dataJA.put(dataJO);
                            ++countmatch;           
                        }else{
                         // client exception "not accepted value for this parameter" 
                        }           */
                    }else{
                            //server exception "wrong filetype"
                    }
                }else{
                    // server exception "server forgot 'required' key"
                }
            }
        }
        
        if(countmatch < userParams.size()){
            //client exception "you put too many parameters. you need to insert values for:..."
        }
        // ITS ALL OK
        postRequest.put("parameters", parameters);
        postRequest.put("data", dataJA);
        return postRequest;
    }    
    
    private static void logJsonObject(JSONObject object) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(object.toString());
        System.out.println(gson.toJson(je));
    }
    
    
}
