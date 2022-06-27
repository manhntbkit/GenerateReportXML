import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

public class Report {
    public static void main(String[] args) throws IOException {
        List<String> reportsName = new ArrayList<>();
        try {
            File myObj = new File("src//data//csv//ReportsName.txt");
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                reportsName.add(myReader.nextLine());
            }
            myReader.close();

        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        //for(String name : reportsName){
            saveReport2File("Opportunity Data for Board");
        //}

    }

    public static void saveReport2File(String reportName) throws IOException {
        String encodedReportName = URLEncoder.encode(reportName, "UTF-8");

        URL url = new URL("https://dws.enablecloud.co.uk/rest/v11/Reports?filter[0][name]=" + encodedReportName);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Authorization", "Bearer a5fe7471-aa78-48c2-86ae-cbc8d4004ffa");

//        con.setDoOutput(true);
//        OutputStream os = con.getOutputStream();
//        OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
//        osw.write("{\n" +
//                "    \"filter\":[\n" +
//                "            {\n" +
//                "                \"name\":\"" +  reportName + "\"\n" +
//                "            }\n" +
//                "        ]\n" +
//                "}");
//        osw.flush();
//        osw.close();
//        os.close();  //don't forget to close the OutputStream

        con.connect();

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        reportName = reportName.replace("/", " ")
                .replace("<", " ")
                .replace(">", " ")
                .replace(":", " ")
                .replace("\\", " ")
                .replace("|", " ")
                .replace("?", " ")
                .replace("*", " ")
                .replace("\"", "");
        BufferedWriter bwr = new BufferedWriter(new FileWriter(new File("src//data//json//" + reportName + ".json")));

        //write contents of StringBuffer to a file
        //only write.
        Gson gson = new GsonBuilder().create();
        SugarResponse sugarRes = gson.fromJson(content.toString(), SugarResponse.class);
        //String jsonString = (String) sugarRes.records.get(0);
        JsonObject jsonObject = gson.toJsonTree(sugarRes.records.get(0)).getAsJsonObject();
        String jsonString = jsonObject.toString();
        bwr.write(jsonString);

        //flush the stream
        bwr.flush();

        //close the stream
        bwr.close();
    }

    public static String getParamsString(Map<String, String> params)
            throws UnsupportedEncodingException{
        StringBuilder result = new StringBuilder();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            result.append("&");
        }

        String resultString = result.toString();
        return resultString.length() > 0
                ? resultString.substring(0, resultString.length() - 1)
                : resultString;
    }



}
