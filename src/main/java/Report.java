import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

public class Report {
    public static void main(String[] args) throws IOException {
        List<String> reportsName = new ArrayList<>();
        try {
            File myObj = new File("src//data//csv//ReportsName.csv");
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                reportsName.add(myReader.nextLine());
            }
            myReader.close();

        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        for(String name : reportsName){
            saveReport2File(name);
        }

    }

    public static void saveReport2File(String reportName) throws IOException {
        URL url = new URL("https://dws.enablecloud.co.uk/rest/v11/Reports");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Content-Type", "application/json");

        con.setRequestProperty("Authorization", "Bearer 0621fab7-9966-44f6-8e91-838f6f9438f7");



        Map<String, List<Map<String, String>>> parameters = new HashMap<>();
        List<Map<String, String>> filters = new ArrayList<Map<String, String>>();
        Map<String, String> filterItemts = new HashMap<>();
        filterItemts.put("name", reportName);
        filters.add(filterItemts);
        parameters.put("filter", filters);

        con.setDoOutput(true);
        DataOutputStream out = new DataOutputStream(con.getOutputStream());
//        out.writeBytes(getParamsString(parameters));
        out.flush();
        out.close();

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
        bwr.write(content.toString());

        //flush the stream
        bwr.flush();

        //close the stream
        bwr.close();
    }
}
