import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WriteXmlDom1 {
    static Map<String, Object> map;
    static Map<String, List<String>>  rowMappingByKey;
    static Document doc;
    static Element reportElement;
    static ContentWrapper content;
    public static void main(String[] args)
            throws ParserConfigurationException, TransformerException, IOException {
        Set<String> names = Stream.of(new File("src//data//json").listFiles())
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .collect(Collectors.toSet());
//        for(String jsonPath : names){
//            json2xml("src//data//json//" + jsonPath);
//        }

//        json2xml("src//data//json//Opportunity Data for Board.json");
//        json2xml("src//data//json//Partner Review, Acct Oppty listing.json");
        json2xml("src//data//json//Tyler's FY23 Attainment (All Products) vs $450k.json");

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
        map = mapper.readValue(dataString, new TypeReference<Map<String,Object>>(){});

        String objectName = (String)map.get("module");

        //convert Java class to XML elements.

        //parse to XML.
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        // root elements
        doc = docBuilder.newDocument();
        reportElement = doc.createElement("Report");
        reportElement.setAttribute("xmlns", "http://soap.sforce.com/2006/04/metadata");
        doc.appendChild(reportElement);

        //region create name, format, reportType, currency
        //name
        createElement("name",(String)map.get("name"), reportElement);
        //format
        String format = (String)map.get("report_type");
        if(format.equals("detailed_summary")){
            format = "Summary";
        }
        createElement("format", format, reportElement);
        //reportType.
        // @TODO: manual create report type in salesforce
        createElement("reportType",convertName((String)map.get("module")), reportElement);
        //currency
        createElement("currency","GBP", reportElement);
        //endregion

        //region create colunms
        List<List<String>> recordsList = ReadCSV.readFile();
        rowMappingByKey = new HashMap<String, List<String>>();
        for(List<String> records: recordsList){
            rowMappingByKey.put(records.get(0), records);
        }

        String contentJson = (String)map.get("content");
        content = new Gson().fromJson(contentJson, ContentWrapper.class);
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
                createColumnElement(rowRecords.get(1));
            }else{
                columnsNotMapped.add(keyMap);
            }
        }
        //endregion

        //region create filter
        ContentWrapper.FiltersDef filtersDef = content.filters_def;
        List<ContentWrapper.Filter> filters = new ArrayList<>();
        /*int filter1Size = filtersDef.Filter_1.entrySet().size();
        Element booleanFilterElement = doc.createElement("booleanFilter");
        if(filter1Size > 2){
            List<String> operators = new ArrayList<>();
            for (Map.Entry<String, Object> entry : filtersDef.Filter_1.entrySet()) {
                if(!entry.getKey().equalsIgnoreCase("operator")) {
                    operators.add(entry.getKey());
                }
            }
            booleanFilterElement.setTextContent(String.join(" AND ", operators));
        }*/

        for (Map.Entry<String, Object> entry : filtersDef.Filter_1.entrySet()) {
            if(!entry.getKey().equalsIgnoreCase("operator")){
                try {
                    filters.add(mapper.convertValue(entry.getValue(), ContentWrapper.Filter.class));
                }catch (Exception e){
                    // try to get child
                    Map<String, Object> filterItems = (Map<String, Object>) entry.getValue();
                    for (Map.Entry<String, Object> filterItem : filterItems.entrySet()) {
                        if(!filterItem.getKey().equalsIgnoreCase("operator")){
                            try {
                                filters.add(mapper.convertValue(filterItem.getValue(), ContentWrapper.Filter.class));
                            }catch (IllegalArgumentException ex){
                                // input_name0 is an array
                                System.out.println("====> input_name0 is an array");
                            }catch (Exception ex){
                                // input_name0 is an array
                                System.out.println("====> " + map.get("name") + ": loi nay ko biet sua");
                            }
                        }
                    }
                }
            }
        }

        Element filterElement = doc.createElement("filter");
        /*if(filter1Size > 2){
            filterElement.appendChild(booleanFilterElement);
        }*/
        for(ContentWrapper.Filter filter : filters){
            String operator = filter.getQualifier_name();
            if(operator.equals("between_dates")){
                ContentWrapper.Filter filter1 = filter;
                filter1.setInput_name0(filter.getInput_name0());
                filter1.setQualifier_name("greaterOrEqual");
                createCriteriaItems(filterElement, filter1, rowMappingByKey, objectName);

                ContentWrapper.Filter filter2 = filter;
                filter2.setInput_name0(filter.getInput_name1());
                filter2.setQualifier_name("lessOrEqual");
                createCriteriaItems(filterElement, filter2, rowMappingByKey, objectName);
            }else if(filter.getName().equals("user_name")){
                ContentWrapper.Filter filter1 = filter;
                filter1.setInput_name0(((ArrayList) filter.getInput_name0()).get(0));
                createCriteriaItems(filterElement, filter1, rowMappingByKey, objectName);
            }else{
                createCriteriaItems(filterElement, filter, rowMappingByKey, objectName);
            }
        }
        reportElement.appendChild(filterElement);
        //endregion

        // region create groupingDown
        Object groupDefs = content.group_defs;
        if(groupDefs instanceof  ArrayList){
            for(ContentWrapper.GroupDef groupDef : (List<ContentWrapper.GroupDef>)groupDefs){
                createGroupingDown(groupDef);
            }
        }
        // endregion

        //region create timeFrameFilter
        Element timeFrameFilterElement = doc.createElement("timeFrameFilter");
        Element dateColumnElement = doc.createElement("dateColumn");
        dateColumnElement.setTextContent("CREATED_DATE");
        timeFrameFilterElement.appendChild(dateColumnElement);

        Element intervalElement = doc.createElement("interval");
        intervalElement.setTextContent("INTERVAL_CUSTOM");
        timeFrameFilterElement.appendChild(intervalElement);

        reportElement.appendChild(timeFrameFilterElement);
        //endregion

        //region create scope, showDetails, showGrandTotal, showSubTotals
        Element scopeElement = doc.createElement("scope");
        scopeElement.setTextContent("organization");
        reportElement.appendChild(scopeElement);

        Element showDetailsElement = doc.createElement("showDetails");
        showDetailsElement.setTextContent("true");
        reportElement.appendChild(showDetailsElement);

        Element showGrandTotalElement = doc.createElement("showGrandTotal");
        showGrandTotalElement.setTextContent("true");
        reportElement.appendChild(showGrandTotalElement);

        Element showSubTotalsElement = doc.createElement("showSubTotals");
        showSubTotalsElement.setTextContent("true");
        reportElement.appendChild(showSubTotalsElement);
        // endregion

        //region write dom document to a file
        String outputFileName = (String)((String) map.get("name"))
                .replace("/", "_")
                .replace("<", "_")
                .replace(">", "_")
                .replace(":", "_")
                .replace("\\", "_")
                .replace("|", "_")
                .replace("?", "_")
                .replace("*", "_")
                .replace(",", "_")
                .replace(" ", "_")
                .replace("__", "_");
        try (FileOutputStream output =
                     new FileOutputStream("src//data//xml//" + outputFileName + ".report-meta.xml")) {
            writeXml(doc, output);
        } catch (IOException | TransformerException e) {
            e.printStackTrace();
        }
        //endregion

        //region log errors
        if(content.display_columns.size() != totalColumn){
            System.out.println("=====================================================================================");
            System.out.println("File name error: " + outputFileName);
            System.out.println("Total column in json: " + content.display_columns.size());
            System.out.println("Total column in xml: " + totalColumn);
            System.out.println("Columns not mapped: " + columnsNotMapped);
            System.out.println("=====================================================================================");
        }
        //endregion
    }

    private static void createGroupingDown(ContentWrapper.GroupDef groupDef){
        Element groupingsDownElement = doc.createElement("groupingsDown");
        createElement("dateGranularity", "Day", groupingsDownElement);
        createElement("sortOrder", "Asc", groupingsDownElement);
        String keyMap = getKeyMap(groupDef.getName(), groupDef.getTable_key(), (String)map.get("module"));
        if(rowMappingByKey.get(keyMap).get(1).equals("Sales_Target__c$Start_date__c")){
            createElement("field", "Sales_Target__c$Fiscal_Year__c", groupingsDownElement);
        }else{
            createElement("field", rowMappingByKey.get(keyMap).get(1), groupingsDownElement);
        }

        reportElement.appendChild(groupingsDownElement);
    }

    private static String getKeyMap(String col, String tableKey, String objectName){

        String keyMap = "";

        if(col.equals("date_entered") || col.equals("user_name")
                || col.equals("full_name")){
            keyMap = col;
        } else if(tableKey.contains(":")){
            keyMap = tableKey.split(":")[1] + ':' + col;
        } else if(tableKey.equals("self")){
            keyMap = objectName + ":" + col;
        }
        return keyMap;
    }
    private static void createCriteriaItems(Element filterElement, ContentWrapper.Filter filter, Map<String, List<String>>  rowMappingByKey, String objectName){
        Element criteriaItemsElement = doc.createElement("criteriaItems");
        String tableKey = filter.getTable_key();
        String col = filter.getName();
        String keyMap = getKeyMap(col, tableKey, objectName);

        // special case for Sales Target
        if(tableKey.equals("ss_Sales_Targets:assigned_user_link")){
            createElement("column", "Sales_Target__c$Assigned_to__c", criteriaItemsElement);
        }else{
            createElement("column", rowMappingByKey.get(keyMap).get(1), criteriaItemsElement);
        }

        createElement("columnToColumn", "false", criteriaItemsElement);
        createElement("isUnlocked", "true", criteriaItemsElement);
        String operator = convertOperator(filter.getQualifier_name());
        if(operator.equals("")){
            System.out.println("==> check filter: " + filter.getQualifier_name());
        }
        createElement("operator", operator, criteriaItemsElement);
        String value;
        if(filter.getInput_name0() instanceof  ArrayList){
            value = String.join(",", (ArrayList)filter.getInput_name0());
            value = value.replaceAll("1 Identified", "Interest");
            value = value.replaceAll("2 Confirming", "Scoping");
            value = value.replaceAll("3 Qualifying", "Scoping");
            value = value.replaceAll("4 Proposing", "Proposal Sent");
            value = value.replaceAll("5 Executing to Win", "Negotiation");
            value = value.replaceAll("6 Contracting", "Contracting");
            value = value.replaceAll("7 Closing", "Contracting");

        }else{
            value = convertValue(filter.getInput_name0().toString());
        }
        createElement("value", value, criteriaItemsElement);

        filterElement.appendChild(criteriaItemsElement);
    }

    private static boolean isValidDate(String inDate) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setLenient(false);
        try {
            dateFormat.parse(inDate.trim());
        } catch (ParseException pe) {
            return false;
        }
        return true;
    }

    private static String convertValue(String inputValue) {
        String returnString = inputValue;
        if(isValidDate(inputValue)){
            try {
                LocalDate d = LocalDate.parse(inputValue);
                returnString = d.getMonthValue() + "/" + d.getDayOfMonth() + "/" + d.getYear();
            } catch (Exception pe) {
                returnString = "";
            }
        }else if(inputValue.equalsIgnoreCase("not_empty")){
            returnString = "";
        }else if(inputValue.length() == 36 && inputValue.replaceAll("-", "").length() == 32){
            // id user in sugar, replace by full name in salesforce
            returnString = String.valueOf(rowMappingByKey.get(inputValue).get(1));
        }
        return returnString;
    }
    private static String convertOperator(String inputOperator){
        String returnString = "";
        switch (inputOperator){
            case "after":
                returnString = "greaterThan";
                break;
            case "one_of":
                returnString = "contains";
                break;
            case "contains":
                returnString = "contains";
                break;
            case "is":
                returnString = "equals";
                break;
            case "empty":
                returnString = "";
                break;
            case "not_empty":
                returnString = "notEqual";
                break;
            case "greaterOrEqual":
                returnString = "greaterOrEqual";
                break;
            case "lessOrEqual":
                returnString = "lessOrEqual";
                break;
            default:
                returnString = "";
        }
        return  returnString;
    }
    private static String convertName(String inputName){
        String returnString = "";
        switch (inputName){
            case "Opportunities":
                returnString = "Opportunity";
            case "ss_Sales_Targets":
                returnString = "Sales_Target_Custom_Report__c";
        }
        return  returnString;

    }
    private static void createElement(String tagName, String tagValue, Element parentElement){
        Element nameElement = doc.createElement(tagName);
        nameElement.setTextContent(tagValue);
        parentElement.appendChild(nameElement);
    }

    private static void createColumnElement(String columnValue){
        Element columnElement = doc.createElement("columns");
        createElement("field",columnValue, columnElement);
        reportElement.appendChild(columnElement);

    }

    // write doc to output stream
    private static void writeXml(Document doc, OutputStream output) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        // pretty print
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(output);
        transformer.transform(source, result);
    }
}