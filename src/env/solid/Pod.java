package solid;

import cartago.Artifact;
import cartago.OPERATION;
import cartago.OpFeedbackParam;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.net.http.HttpRequest;

/**
 * A CArtAgO artifact that agent can use to interact with LDP containers in a Solid pod.
 */
public class Pod extends Artifact {

    private String podURL; // the location of the Solid pod 

  /**
   * Method called by CArtAgO to initialize the artifact. 
   *
   * @param podURL The location of a Solid pod
   */
    public void init(String podURL) {
        this.podURL = podURL;
        log("Pod artifact initialized for: " + this.podURL);
    }

  /**
   * CArtAgO operation for creating a Linked Data Platform container in the Solid pod
   *
   * @param containerName The name of the container to be created
   * 
   */
    @OPERATION
    public void createContainer(String containerName) {   
            if (resourceExists("https://solid.interactions.ics.unisg.ch/fabiog/" + containerName)) {
                // container already exists, no need to create
                return;
            }
            
            // create request body in turtle syntax, according to https://www.w3.org/TR/ldp-primer/#creating-containers-and-structural-hierarchy 2.3
            String containerDescription = "Container created by agents";
            String content = "@prefix ldp: <http://www.w3.org/ns/ldp#>.\n"+
            "@prefix dcterms: <http://purl.org/dc/terms/>.\n" +
            "<> a ldp:Container, ldp:BasicContainer, ldp:Resource;\n" +
            "dcterms:title \"" + containerName + "\";\n" +
            "dcterms:description \"" + containerDescription + "\" .";

            HttpClient client = HttpClient.newHttpClient();
            String url = "https://solid.interactions.ics.unisg.ch/fabiog/";
            String contentType = "text/turtle";
            String linkHeader = "<http://www.w3.org/ns/ldp/BasicContainer>; rel=\"type\"";
            String slugHeader = containerName + "/";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(content, StandardCharsets.UTF_8))
                .header("Content-Type", contentType)
                .header("Link", linkHeader)
                .header("Slug", slugHeader)
                .build();
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                log(response.body());
        
                if(response.statusCode() == 201) {
                    log("Container created");
                } else {
                    log("Error while creating container: " + response.body());
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
    }

  /**
   * CArtAgO operation for publishing data within a .txt file in a Linked Data Platform container of the Solid pod
   * 
   * @param containerName The name of the container where the .txt file resource will be created
   * @param fileName The name of the .txt file resource to be created in the container
   * @param data An array of Object data that will be stored in the .txt file
   */
    @OPERATION
    public void publishData(String containerName, String fileName, Object[] data) {
        String content = String.join("\n", Arrays.stream(data).toArray(String[]::new));
        // content = createStringFromArray(data);

        HttpClient client = HttpClient.newHttpClient();
        String url = "https://solid.interactions.ics.unisg.ch/fabiog/" + containerName + "/" + fileName;

        String contentType = "text/plain";

        HttpRequest request;

        // check if file already exists, if the file exists write to file with PUT, otherwise create the file with POST
        if (resourceExists(url)) {
            request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .PUT(HttpRequest.BodyPublishers.ofString(content, StandardCharsets.UTF_8))
            .header("Content-Type", contentType)
            .build();
        } else {
            url = "https://solid.interactions.ics.unisg.ch/fabiog/" + containerName;
            request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .POST(HttpRequest.BodyPublishers.ofString(content, StandardCharsets.UTF_8))
            .header("Content-Type", contentType)
            .build();
        }

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            log(response.body());
    
            if(response.statusCode() == 200 || response.statusCode() == 201) {
                log("File writing successful");
            } else {
                log("Error while writing file: " + response.body());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

  /**
   * CArtAgO operation for reading data of a .txt file in a Linked Data Platform container of the Solid pod
   * 
   * @param containerName The name of the container where the .txt file resource is located
   * @param fileName The name of the .txt file resource that holds the data to be read
   * @param data An array whose elements are the data read from the .txt file
   */
    @OPERATION
    public void readData(String containerName, String fileName, OpFeedbackParam<Object[]> data) {
        data.set(readData(containerName, fileName));
    }

  /**
   * Method for reading data of a .txt file in a Linked Data Platform container of the Solid pod
   * 
   * @param containerName The name of the container where the .txt file resource is located
   * @param fileName The name of the .txt file resource that holds the data to be read
   * @return An array whose elements are the data read from the .txt file
   */
    public Object[] readData(String containerName, String fileName) {
        HttpClient client = HttpClient.newHttpClient();
        String url = "https://solid.interactions.ics.unisg.ch/fabiog/" + containerName + "/" + fileName;
        String contentType = "text/plain";
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .header("Content-Type", contentType)
            .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    
            if(response.statusCode() == 200) {
                log("Data read successfully");
                return createArrayFromString(response.body());
            } else {
                log("Error while reading data: " + response.body());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new Object[0];
    }

    private Boolean resourceExists(String resource) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(resource))
            .GET()
            .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    
            if(response.statusCode() == 200) {
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

  /**
   * Method that converts an array of Object instances to a string, 
   * e.g. the array ["one", 2, true] is converted to the string "one\n2\ntrue\n"
   *
   * @param array The array to be converted to a string
   * @return A string consisting of the string values of the array elements separated by "\n"
   */
    public static String createStringFromArray(Object[] array) {
        StringBuilder sb = new StringBuilder();
        for (Object obj : array) {
            sb.append(obj.toString()).append("\n");
        }
        return sb.toString();
    }

  /**
   * Method that converts a string to an array of Object instances computed by splitting the given string with delimiter "\n"
   * e.g. the string "one\n2\ntrue\n" is converted to the array ["one", "2", "true"]
   *
   * @param str The string to be converted to an array
   * @return An array consisting of string values that occur by splitting the string around "\n"
   */
    public static Object[] createArrayFromString(String str) {
        return str.split("\n");
    }


  /**
   * CArtAgO operation for updating data of a .txt file in a Linked Data Platform container of the Solid pod
   * The method reads the data currently stored in the .txt file and publishes in the file the old data along with new data 
   * 
   * @param containerName The name of the container where the .txt file resource is located
   * @param fileName The name of the .txt file resource that holds the data to be updated
   * @param data An array whose elements are the new data to be added in the .txt file
   */
    @OPERATION
    public void updateData(String containerName, String fileName, Object[] data) {
        Object[] oldData = readData(containerName, fileName);
        Object[] allData = new Object[oldData.length + data.length];
        System.arraycopy(oldData, 0, allData, 0, oldData.length);
        System.arraycopy(data, 0, allData, oldData.length, data.length);
        publishData(containerName, fileName, allData);
    }
}
