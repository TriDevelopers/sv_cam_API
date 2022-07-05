import java.io.File;
import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JOptionPane;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class gather {
    public static void main(String[] args) throws Exception {	

        // ObjectMapper instantiation
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // This object holds all info gathered and infoList to hold all camera information
        infoGathered info = new infoGathered();
        List<infoGathered> infoList = new LinkedList<>();

        // This will be the output file
        File file = new File("output.json"); 

        // This is Camera Code that we input
        String CameraCodeInput = JOptionPane.showInputDialog("Camera Code is:");

        // Decode config file
        String fileName = "C:/Users/ndmt2/Desktop/Cam_API/sv_cam_API/config/config.json";
        File fis = new File(fileName);
        
        config configFile = objectMapper.readValue(fis, config.class);

        // API call 5_GetListCamera
        String link = "";
        String configData[] = findConfig(objectMapper, "GetListCamera", configFile);

        link = "http://" + configData[0] + ":" + configData[1] + "/" + configData[2] + "/"; 

        HttpRequest request5 = HttpRequest.newBuilder()
            .uri(URI.create(link +"cctv_camera"))
            .header("X-Request-ID", configData[3])
            .header("X-API-Key", configData[4])
            .method("GET", HttpRequest.BodyPublishers.noBody())
            .build();
        HttpResponse<String> response5 = HttpClient.newHttpClient().send(request5, HttpResponse.BodyHandlers.ofString());

        // Deserialization into the `cameraInfo` class, 
        cameraInfo camera = objectMapper.readValue(response5.body(), cameraInfo.class);

        // Get name of the camera for multiple camera of the same code
        for (int i = 0; i < camera.ds.size(); i++) {
            if (camera.ds.get(i).Code.equals(CameraCodeInput)) {
                info = new infoGathered();

                // Assign info to the info obejct
                info.Cam_Code = camera.ds.get(i).Code;
                info.Name = camera.ds.get(i).Name;
                info.Branch_Code = camera.ds.get(i).Branch_Id;               

                // API call 3_GetBranchByCode
                configData = findConfig(objectMapper, "GetBranchByCode", configFile);
                link = "http://" + configData[0] + ":" + configData[1] + "/" + configData[2] + "/"; 

                HttpRequest request3 = HttpRequest.newBuilder()
                    .uri(URI.create(link + "cctv_branch?dk=%7B%22Code%22%3A%22" + info.Branch_Code.replaceAll(" ", "") + "%22%7D"))
                    .header("X-Request-ID", configData[3])
                    .header("X-API-Key", configData[4])
                    .method("GET", HttpRequest.BodyPublishers.noBody())
                    .build();
                HttpResponse<String> response3 = HttpClient.newHttpClient().send(request3, HttpResponse.BodyHandlers.ofString());

                // Deserialization into the `branchInfo` class
                branchInfo branch = objectMapper.readValue(response3.body(), branchInfo.class);
                
                // Some branch not exist in any company, skip it
                if (branch.ds.isEmpty()){
                    continue;
                }

                // Assign info to the info obejct
                info.Branch_Name = branch.ds.get(0).Name;
                info.Company_Code = branch.ds.get(0).Company_Code;

                // API call 1_GetCompanyByCode
                configData = findConfig(objectMapper, "GetCompanyByCode", configFile);;
                link = "http://" + configData[0] + ":" + configData[1] + "/" + configData[2] + "/"; 

                HttpRequest request1 = HttpRequest.newBuilder()
                    .uri(URI.create(link + "cctv_company?dk=%7B%22Code%22%3A%22" + info.Company_Code.replaceAll(" ", "") + "%22%7D"))
                    .header("X-Request-ID", configData[3])
                    .header("X-API-Key", configData[4])
                    .method("GET", HttpRequest.BodyPublishers.noBody())
                    .build();
                HttpResponse<String> response1 = HttpClient.newHttpClient().send(request1, HttpResponse.BodyHandlers.ofString());

                // Deserialization into the `companyInfo` class
                companyInfo company = objectMapper.readValue(response1.body(), companyInfo.class);

                // If we can't find the company using company code, skip it
                if (company.ds.isEmpty()) {
                    continue;
                }
                    
                // Assign info to the info obejct
                info.Company_Name = company.ds.get(0).Name;

                infoList.add(info);
            } 
        }
        // output all info to output.json
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, infoList);
    }

    // Function to get all information needed from the config file
    public static String[] findConfig(ObjectMapper objectMapper, String name, config configFile) throws StreamReadException, DatabindException, IOException {
        String [] result = new String[5];
        
        for (int i = 0; i < configFile.list_servers.size(); i++) {
            if (configFile.list_servers.get(i).name.equals(name)) {
                result[0] = configFile.list_servers.get(i).server;
                result[1] = configFile.list_servers.get(i).port;
                result[2] = configFile.list_servers.get(i).version;
                result[3] = configFile.list_servers.get(i).Request_ID;
                result[4] = configFile.list_servers.get(i).API_Key;
            }
        }
        return result;
    }
}
