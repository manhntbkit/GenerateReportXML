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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import com.github.underscore.U;
import org.xml.sax.InputSource;

public class WriteXmlDom1 {

    public static void main(String[] args)
            throws ParserConfigurationException, TransformerException, IOException {

        //read json file.
        String dataString = "";
        try {

            File myObj = new File("src//data//json//response1.json");
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                dataString += myReader.nextLine();
            }
            myReader.close();

        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        //convert json string to Java class.
//        ObjectMapper mapper = new ObjectMapper();
//        JSONWrapper JSONwrap = mapper.readValue(dataString, JSONWrapper.class);

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> map
                = mapper.readValue(dataString, new TypeReference<Map<String,Object>>(){});

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            System.out.println("Key:: " + entry.getKey() + " Value : " + entry.getValue());
        }
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
        for (ContentWrapper.ColumnObj col: content.display_columns) {
            String tableKey = col.getTable_key();
            String keyMap = "";
            if(tableKey.contains(":")){
                keyMap = tableKey.split(":")[1] + ':' + col.getName();
            }
            if(tableKey.equals("self")){
                keyMap = objectName + ":" + col.getName();
            }
            //else{
                //keyMap = tableKey + ':' + col.getName();
            //}

            //:accounts:name => ACCOUNT_NAME
            List<String> rowRecords = rowMappingByKey.get(keyMap);
            if(rowRecords != null && rowRecords.size() == 2){
                createColumnElement(doc,rowRecords.get(1), reportElement);
            }

//            if(tableKey.contains(":accounts") && "name".equals(col.getName())  ){
//                createColumnElement(doc,"ACCOUNT_NAME", reportElement);
//            }
//            if(tableKey.contains(":accounts") && "region_c".equals(col.getName())  ){
//                createColumnElement(doc,"Account.DWS_Region__c", reportElement);
//            }
//            if(tableKey.contains(":accounts") && "account_type".equals(col.getName())  ){
//                createColumnElement(doc,"ACCOUNT_TYPE", reportElement);
//            }

        }

        // write dom document to a file
        try (FileOutputStream output =
                     new FileOutputStream("src//data//xml//staff-dom.xml")) {
            writeXml(doc, output);
        } catch (IOException e) {
            e.printStackTrace();
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