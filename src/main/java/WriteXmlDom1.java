import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.underscore.U;
import org.xml.sax.InputSource;

public class WriteXmlDom1 {

    public static void main(String[] args)
            throws ParserConfigurationException, TransformerException, IOException {
        Set<String> names = Stream.of(new File("src//data//json").listFiles())
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .collect(Collectors.toSet());
        for(String jsonPath : names){
            json2xml("src//data//json//" + jsonPath);
        }
    }

    private static void json2xml(String jsonPath) throws JsonProcessingException, ParserConfigurationException {
        //read json file.
        String dataString = "";
        try {
            File myObj = new File(jsonPath);
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                dataString += myReader.nextLine();
            }
            myReader.close();

        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
        Map<String, Object> map
                = mapper.readValue(dataString, new TypeReference<Map<String,Object>>(){});

        String objectName = (String)map.get("module");

        //convert Java class to XML elements.

        //parse to XML.
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        // root elements
        Document doc = docBuilder.newDocument();
        Element reportElement = doc.createElement("Report");
        reportElement.setAttribute("xmlns", "http://soap.sforce.com/2006/04/metadata");
        doc.appendChild(reportElement);

        //name
        createElement(doc, "name",(String)map.get("name"), reportElement);
        //format
        createElement(doc, "format",(String)map.get("report_type"), reportElement);
        //reportType
        createElement(doc, "reportType",convertName((String)map.get("module")), reportElement);
        //currency
        createElement(doc, "currency","GBP", reportElement);

        //create colunms:
        List<List<String>> recordsList = ReadCSV.readFile();
        Map<String, List<String>>  rowMappingByKey = new HashMap<String, List<String>>();
        for(List<String> records: recordsList){
            rowMappingByKey.put(records.get(0), records);
        }

        String contentJson = (String)map.get("content");
        ContentWrapper content = new Gson().fromJson(contentJson, ContentWrapper.class);
        int totalColumn = 0;
        List<String> columnsNotMapped = new ArrayList<>();
        for (ContentWrapper.ColumnObj col: content.display_columns) {
            String tableKey = col.getTable_key();
            String keyMap = "";
            if(col.getName().equals("date_entered") || col.getName().equals("user_name")
                    || col.getName().equals("full_name")){
                keyMap = col.getName();
            } else if(tableKey.contains(":")){
                keyMap = tableKey.split(":")[1] + ':' + col.getName();
            } else if(tableKey.equals("self")){
                keyMap = objectName + ":" + col.getName();
            }
            //else{
            //keyMap = tableKey + ':' + col.getName();
            //}

            //:accounts:name => ACCOUNT_NAME
            List<String> rowRecords = rowMappingByKey.get(keyMap);
            if(rowRecords != null && rowRecords.size() == 2){
                totalColumn++;
                createColumnElement(doc,rowRecords.get(1), reportElement);
            }else{
                columnsNotMapped.add(keyMap);
            }
        }

        // write dom document to a file
        String outputFileName = (String)((String) map.get("name"))
                .replace("/", " ")
                .replace("<", " ")
                .replace(">", " ")
                .replace(":", " ")
                .replace("\\", " ")
                .replace("|", " ")
                .replace("?", " ")
                .replace("*", " ");
        try (FileOutputStream output =
                     new FileOutputStream("src//data//xml//" + outputFileName + ".xml")) {
            writeXml(doc, output);
        } catch (IOException | TransformerException e) {
            e.printStackTrace();
        }

        // log errors
        if(content.display_columns.size() != totalColumn){
            System.out.println("=====================================================================================");
            System.out.println("File name error: " + outputFileName);
            System.out.println("Total column in json: " + content.display_columns.size());
            System.out.println("Total column in xml: " + totalColumn);
            System.out.println("Columns not mapped: " + columnsNotMapped);
            System.out.println("=====================================================================================");
        }
    }

    private static String convertName(String inputName){
        String returnString = "";
        switch (inputName){
            case "Opportunities":
                returnString = "Opportunity";
        }
        return  returnString;

    }
    private static void createElement(Document doc, String tagName, String tagValue, Element parentElement){
        Element nameElement = doc.createElement(tagName);
        nameElement.setTextContent(tagValue);
        parentElement.appendChild(nameElement);
    }

    private static void createColumnElement(Document doc, String columnValue, Element reportElement){
        Element columnElement = doc.createElement("columns");
        createElement(doc, "field",columnValue, columnElement);
        reportElement.appendChild(columnElement);

    }

    // write doc to output stream
    private static void writeXml(Document doc,
                                 OutputStream output)
            throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        // pretty print
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(output);
        transformer.transform(source, result);
    }
}