import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
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
    static int indexReportName = 1;
    public static void main(String[] args)
            throws ParserConfigurationException, TransformerException, IOException {
        Set<String> names = Stream.of(new File("src//data//json").listFiles())
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .collect(Collectors.toSet());
        for(String jsonPath : names){
            json2xml("src//data//json//" + jsonPath);
        }

//        json2xml("src//data//json//Opportunity Data for Board.json");
//        json2xml("src//data//json//Partner Review, Acct Oppty listing.json");
//        json2xml("src//data//json//Tyler's FY23 Attainment (All Products) vs $450k.json");
//        json2xml("src//data//json//Bill's pipeline.json");
//        json2xml("src//data//json//Closed count by month and product service (last this FQ).json");
//        json2xml("src//data//json//KJ DATA CLEANUP  Opportunities key fields check (Bill).json");
//        json2xml("src//data//json//Closed count by month and user (last this fiscal qtr).json");
//        json2xml("src//data//json//Closed value by month and product service (last this FQ).json");
//        json2xml("src//data//json//KJ DATA CLEANUP  Accounts to be reassigned.json");
//        json2xml("src//data//json//KJ DATA CLEANUP  Opportunities key fields check (Bill).json");
//        json2xml("src//data//json//KJ DATA CLEANUP  Opportunities key fields check (Pat).json");
//        json2xml("src//data//json//NA End User JDE Accounts w Contacts.json");
//        json2xml("src//data//json//Non-NA End User JDE Accounts w Contacts.json");
//        json2xml("src//data//json//LogiGear, REVENUE last 60 days.json");
//        json2xml("src//data//json//Closed won values FY21, FQ type.json");
//        json2xml("src//data//json//Annual Product Renewals by Month.json");
//        json2xml("src//data//json//ARR Tempo pipeline by month.json");
        json2xml("src//data//json//Closed count by month and user (last this fiscal qtr).json");
    }


    private static void json2xml(String jsonPath) throws JsonProcessingException, ParserConfigurationException {
        List<List<String>> recordsList = ReadCSV.readFile();
        rowMappingByKey = new CaseInsensitiveMap<>();
        for(List<String> records: recordsList){
            rowMappingByKey.put(records.get(0), records);
        }

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
        String contentJson = (String)map.get("content");
        content = new Gson().fromJson(contentJson, ContentWrapper.class);
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

        //region create chart, name, format, reportType, currency
        //chart
        try {
            String chartType = content.chart_type;
            if (chartType.equals("vGBarF") || chartType.equals("funnelF") || chartType.equals("vBarF")) {
                List<String> chartColums = List.of(content.numerical_chart_column.split(":"));
                if(chartColums.size() == 3){
                    // content.numerical_chart_column = self:yr1_net_contract_value_gbp_c:sum
                    String keyMap = getKeyMap(chartColums.get(1), chartColums.get(0), (String) map.get("module"));
                    List<String> rowRecords = rowMappingByKey.get(keyMap);

                    String operator = chartColums.get(2);
                    String groupingColumn = "";
                    Object groupDefs = content.group_defs;
                    if (groupDefs instanceof ArrayList) {;
                        List<String> groupingColumns = new ArrayList<>();
                        for(ContentWrapper.GroupDef group : content.group_defs) {
                            if("Opportunities".equals(map.get("module")) && group.getName().equals("actual_close_date_c") && group.getColumn_function().equals("fiscalQuarter")){
                                groupingColumns.add("Opportunity.Fiscal_Quarter_Closed_Date__c");
                            }else if("Opportunities".equals(map.get("module")) && group.getName().equals("actual_close_date_c") && group.getColumn_function().equals("fiscalYear")){
                                groupingColumns.add("Opportunity.Fiscal_Year_Close_Date__c");
                            }else if("Opportunities".equals(map.get("module")) && group.getName().equals("actual_close_date_c") && group.getColumn_function().equals("quarter")){
                                groupingColumns.add("Opportunity.Quarter_Close_Date__c");
                            }else if("Opportunities".equals(map.get("module")) && group.getName().equals("actual_close_date_c") && group.getColumn_function().equals("month")){
                                groupingColumns.add("Opportunity.Month_Close_Date__c");
                            }else if("Opportunities".equals(map.get("module")) && group.getName().equals("date_closed") && group.getColumn_function().equals("fiscalQuarter")){
                                groupingColumns.add("Opportunity.Fiscal_Quarter_Expected_Closed_Date__c");
                            }else if("Opportunities".equals(map.get("module")) && group.getName().equals("date_closed") && group.getColumn_function().equals("week")){
                                groupingColumns.add("Opportunity.Week_Expected_Close_Date__c");
                            }else if("Opportunities".equals(map.get("module")) && group.getName().equals("date_closed") && group.getColumn_function().equals("month")){
                                groupingColumns.add( "Opportunity.Month_Expected_Close_Date__c");
                            }else if ("ss_Sales_Targets".equals(map.get("module")) && group.getName().equals("start_date")) {
                                groupingColumns.add("Sales_Target__c$Fiscal_Year__c");
                            } else {
                                keyMap = getKeyMap(group.getName(), group.getTable_key(), (String) map.get("module"));
                                groupingColumns.add(rowMappingByKey.get(keyMap).get(1));
                            }
                        }
                        groupingColumn = String.join(";", groupingColumns);
                    }
                    String chartColumn = rowRecords.get(1);
                    createChart(chartColumn, operator, groupingColumn, chartType, chartColums.get(2));
                }else if(chartColums.size() == 2){
                    // @TODO: content.numerical_chart_column = self:count
//                    System.out.println("===> check here: report name: " + map.get("name"));

                    String operator = chartColums.get(1);
                    String groupingColumn = "";
                    Object groupDefs = content.group_defs;
                    if (groupDefs instanceof ArrayList) {;
                        List<String> groupingColumns = new ArrayList<>();
                        for(ContentWrapper.GroupDef group : content.group_defs) {
                            if("Opportunities".equals(map.get("module")) && group.getName().equals("actual_close_date_c") && group.getColumn_function().equals("fiscalQuarter")){
                                groupingColumns.add("Opportunity.Fiscal_Quarter_Closed_Date__c");
                            }else if("Opportunities".equals(map.get("module")) && group.getName().equals("actual_close_date_c") && group.getColumn_function().equals("fiscalYear")){
                                groupingColumns.add("Opportunity.Fiscal_Year_Close_Date");
                            }else if("Opportunities".equals(map.get("module")) && group.getName().equals("actual_close_date_c") && group.getColumn_function().equals("quarter")){
                                groupingColumns.add("Opportunity.Quarter_Close_Date__c");
                            }else if("Opportunities".equals(map.get("module")) && group.getName().equals("actual_close_date_c") && group.getColumn_function().equals("month")){
                                groupingColumns.add("Opportunity.Month_Close_Date__c");
                            }else if("Opportunities".equals(map.get("module")) && group.getName().equals("date_closed") && group.getColumn_function().equals("fiscalQuarter")){
                                groupingColumns.add("Opportunity.Fiscal_Quarter_Expected_Closed_Date__c");
                            }else if("Opportunities".equals(map.get("module")) && group.getName().equals("date_closed") && group.getColumn_function().equals("week")){
                                groupingColumns.add("Opportunity.Week_Expected_Close_Date__c");
                            }else if("Opportunities".equals(map.get("module")) && group.getName().equals("date_closed") && group.getColumn_function().equals("month")){
                                groupingColumns.add( "Opportunity.Month_Expected_Close_Date__c");
                            }else if ("ss_Sales_Targets".equals(map.get("module")) && group.getName().equals("start_date")) {
                                groupingColumns.add("Sales_Target__c$Fiscal_Year__c");
                            } else {
                                String keyMap = getKeyMap(group.getName(), group.getTable_key(), (String) map.get("module"));
                                groupingColumns.add(rowMappingByKey.get(keyMap).get(1));
                            }
                        }
                        groupingColumn = String.join(";", groupingColumns);
                    }
                    String chartColumn = "CREATED_DATE";
                    createChart(chartColumn, operator, groupingColumn, chartType, chartColums.get(1));

                }else{
                    // @TODO: exception, continue check
                    System.out.println("===> exception, continue check: report name: " + map.get("name"));
                }
            }else if(chartType != null && !chartType.isBlank() && !chartType.equals("none")
            && !chartType.equals("vGBarF") && !chartType.equals("funnelF") && !chartType.equals("vBarF")){
                // log report type need to handle
                System.out.println("Check chart type: " + chartType);
            }
        }catch (Exception e){
            System.out.println("===> loi ne. " + e.getMessage() + e.getStackTrace());
        }

        //name
        String reportName = (String)map.get("name");
        if(reportName.length() > 40) {
            reportName = reportName.substring(0, 36) + " " + indexReportName;
            indexReportName++;
        }
        createElement("name", reportName, reportElement);
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
        createElement("description", (String)map.get("name"), reportElement);
        //endregion

        //region create colunms



        int totalColumn = 0;
        List<String> columnsNotMapped = new ArrayList<>();
        for (ContentWrapper.ColumnObj col: content.display_columns) {
            Boolean isSkip = false;
            for(ContentWrapper.GroupDef group_def : content.group_defs){
                if(group_def.getName().equals(col.getName()) ||
                        (group_def.getName().equals("user_name") && col.getName().equals("full_name")) ||
                        (group_def.getName().equals("full_name") && col.getName().equals("user_name"))){
                    isSkip = true;
                    break;
                }
            }
            if(isSkip) {
                totalColumn++; // to ignore print error Columns not mapped
                continue;
            }
            String tableKey = col.getTable_key();
            String keyMap = "";
            if (col.getName().equals("date_entered") || col.getName().equals("user_name")
                    || col.getName().equals("full_name") || col.getName().equals("first_name")
                    || col.getName().equals("last_name") || col.getName().equals("date_modified")) {
                keyMap = col.getName();
            } else if (tableKey.contains(":")) {
                keyMap = tableKey.split(":")[1] + ':' + col.getName();
            } else if (tableKey.equals("self")) {
                keyMap = objectName + ":" + col.getName();
            }
            //else{
            //keyMap = tableKey + ':' + col.getName();
            //}

            //:accounts:name => ACCOUNT_NAME
            List<String> rowRecords = rowMappingByKey.get(keyMap);
            if (rowRecords != null && rowRecords.size() == 2) {
                totalColumn++;
                String aggregateTypes = "";
                for(ContentWrapper.SummaryColumn summaryColumn : content.summary_columns){
                    if(col.getName().equals(summaryColumn.getName())){
                        aggregateTypes = summaryColumn.getGroup_function();
                        if(aggregateTypes.equals("avg")){
                            aggregateTypes = "average";
                        }
                    }
                }
                createColumnElement(rowRecords.get(1), aggregateTypes);
            } else {
                columnsNotMapped.add(keyMap);
            }
        }
        //endregion

        //region create filter
        ContentWrapper.FiltersDef filtersDef = content.filters_def;
        List<ContentWrapper.Filter> filters = new ArrayList<>();
        // combine conditional
        Element booleanFilterElement = doc.createElement("booleanFilter");
        String finalLogicFilter = "";
        List<String> logicFilter = new ArrayList<>();

        WrapInt startNumber = new WrapInt(1);
        for (Map.Entry<String, Object> entry : filtersDef.Filter_1.entrySet()) {
            if(!entry.getKey().equals("operator")){
                logicFilter.add(generateLogicFilter((Map<String, Object>) entry.getValue(), startNumber));
            }else {
                finalLogicFilter = String.join(" " + entry.getValue() + " ", logicFilter);
            }
        }
        booleanFilterElement.setTextContent(finalLogicFilter);
        // combine conditional

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
        // combine conditional
        if(!booleanFilterElement.getTextContent().isEmpty()){
            filterElement.appendChild(booleanFilterElement);
        }

        // combine conditional

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
                try{
                ContentWrapper.Filter filter1 = filter;
                if(filter.getInput_name0() instanceof ArrayList) {
                    filter1.setInput_name0(((ArrayList) filter.getInput_name0()).get(0));
                }else{
                    filter1.setInput_name0(filter.getInput_name0());
                }
                createCriteriaItems(filterElement, filter1, rowMappingByKey, objectName);
                }catch (Exception e){
                    System.out.println(e);
                }
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
        try {
            Element timeFrameFilterElement = doc.createElement("timeFrameFilter");
            Element dateColumnElement = doc.createElement("dateColumn");
                if (!rowMappingByKey.get((String) map.get("module")).get(1).contains("__c")) {
                    dateColumnElement.setTextContent("CREATED_DATE");
                } else {
                    dateColumnElement.setTextContent(rowMappingByKey.get((String) map.get("module")).get(1) + "$CreatedDate");
                }

                timeFrameFilterElement.appendChild(dateColumnElement);

                Element intervalElement = doc.createElement("interval");
                intervalElement.setTextContent("INTERVAL_CUSTOM");
                timeFrameFilterElement.appendChild(intervalElement);

            reportElement.appendChild(timeFrameFilterElement);
        }catch (Exception e){
            System.out.println(e);
        }
        //endregion

        //region create scope, showDetails, showGrandTotal, showSubTotals
        Element scopeElement = doc.createElement("scope");
        scopeElement.setTextContent("organization");
        reportElement.appendChild(scopeElement);

        Element showDetailsElement = doc.createElement("showDetails");
        String isShowDetail = content.display_columns.size() > 0 ? "true" : "false";
        showDetailsElement.setTextContent(isShowDetail);
        reportElement.appendChild(showDetailsElement);

        Element showGrandTotalElement = doc.createElement("showGrandTotal");
        showGrandTotalElement.setTextContent("true");
        reportElement.appendChild(showGrandTotalElement);

        Element showSubTotalsElement = doc.createElement("showSubTotals");
        showSubTotalsElement.setTextContent("true");
        reportElement.appendChild(showSubTotalsElement);
        // endregion

        //region write dom document to a file
        String outputFileName = ((String) map.get("name"))
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
                .replace("(", "_")
                .replace(")", "_")
                .replace("+", "_")
                .replace(".", "_")
                .replace("$", "usd")
                .replace("£", "pound")
                .replace("-", "_")
                .replace("&", "_")
                .replace("'", "_")
                .replace("___", "_")
                .replace("__", "_")
                + ".report-meta.xml";
        outputFileName = outputFileName.replace("_.", ".");
        try (FileOutputStream output =
                     new FileOutputStream("src//data//xml//" + outputFileName)) {
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

    private static String generateLogicFilter(Map<String, Object> filter, WrapInt startNumber){
        String logicFilter = "";
        String operator = "";
        List<Integer> conditionItems = new ArrayList<>();
        for (Map.Entry<String, Object> entry : filter.entrySet()) {
            if(!entry.getKey().equalsIgnoreCase("operator")) {

                conditionItems.add(startNumber.getValue());
                startNumber.setValue(startNumber.getValue() + 1);
            }else if(entry.getKey().equalsIgnoreCase("operator")){
                operator = (String) entry.getValue();
            }
        }
        logicFilter = "(" + conditionItems.stream().map(String::valueOf).collect(Collectors
                .joining(" " + operator + " ")) + ")";
        return logicFilter;
    }

    private static void createChart(String chartColumn, String operator, String groupingColumn, String chartType, String summaryType){
        Element chartElement = doc.createElement("chart");
        createElement("backgroundColor1", "#FFFFFF", chartElement);
        createElement("backgroundColor2", "#FFFFFF", chartElement);
        createElement("backgroundFadeDir", "Diagonal", chartElement);

        Element chartSummariesElement = doc.createElement("chartSummaries");
        if(summaryType.equalsIgnoreCase("sum")){
            createElement("aggregate", operator, chartSummariesElement);
            createElement("axisBinding", "y", chartSummariesElement);
            createElement("column", chartColumn, chartSummariesElement);
        }else if(summaryType.equalsIgnoreCase("count")){
            createElement("axisBinding", "y", chartSummariesElement);
            createElement("column", "RowCount", chartSummariesElement);
        }

        chartElement.appendChild(chartSummariesElement);

        if(chartType.equals("vGBarF")){
            chartType = "VerticalColumn";
        }else if(chartType.equals("funnelF")){
            chartType = "Funnel";
        }else if(chartType.equals("vBarF")){
            chartType = "VerticalColumnStacked";
        }
        createElement("chartType", chartType, chartElement);
        createElement("enableHoverLabels", "false", chartElement);
        String expandOthers = "true";
        if(chartType.equals("VerticalColumn")){
            expandOthers = "true";
        }else if(chartType.equals("Funnel")){
            expandOthers = "false";
        }
        createElement("expandOthers", expandOthers, chartElement);
        List<String> groupingColumns = List.of(groupingColumn.split(";"));
        createElement("groupingColumn", groupingColumns.get(0), chartElement);
        if(chartType.equals("Funnel")){
            createElement("legendPosition", "Right", chartElement);
        }

        createElement("location", "CHART_TOP", chartElement);

        if(chartType.equals("VerticalColumnStacked")){
            createElement("secondaryGroupingColumn", groupingColumns.get(1), chartElement);
        }

        createElement("showAxisLabels", "true", chartElement);
        createElement("showPercentage", "false", chartElement);
        createElement("showTotal", "false", chartElement);
        String showValue = "false";
        if(chartType.equals("VerticalColumn")){
            showValue = "false";
        }else if(chartType.equals("Funnel")){
            showValue = "true";
        }
        createElement("showValues", showValue, chartElement);
        createElement("size", "Medium", chartElement);
        createElement("summaryAxisRange", "Auto", chartElement);
        createElement("textColor", "#000000", chartElement);
        createElement("textSize", "12", chartElement);
        createElement("titleColor", "#000000", chartElement);
        createElement("titleSize", "18", chartElement);
        reportElement.appendChild(chartElement);
    }
    private static void createGroupingDown(ContentWrapper.GroupDef groupDef){
        Element groupingsDownElement = doc.createElement("groupingsDown");
        createElement("dateGranularity", "Day", groupingsDownElement);
        createElement("sortOrder", "Asc", groupingsDownElement);
        String keyMap = getKeyMap(groupDef.getName(), groupDef.getTable_key(), (String)map.get("module"));
        try{
            if(rowMappingByKey.get(keyMap).get(1).equals("Sales_Target__c$Start_date__c")){
                createElement("field", "Sales_Target__c$Fiscal_Year__c", groupingsDownElement);
            }else if("Opportunities".equals(map.get("module")) && groupDef.getName().equals("actual_close_date_c") && groupDef.getColumn_function().equals("fiscalQuarter")){
                createElement("field", "Opportunity.Fiscal_Quarter_Closed_Date__c", groupingsDownElement);
            }else if("Opportunities".equals(map.get("module")) && groupDef.getName().equals("actual_close_date_c") && groupDef.getColumn_function().equals("fiscalYear")){
                createElement("field", "Opportunity.Fiscal_Year_Close_Date__c", groupingsDownElement);
            }else if("Opportunities".equals(map.get("module")) && groupDef.getName().equals("actual_close_date_c") && groupDef.getColumn_function().equals("quarter")){
                createElement("field", "Opportunity.Quarter_Close_Date__c", groupingsDownElement);
            }else if("Opportunities".equals(map.get("module")) && groupDef.getName().equals("actual_close_date_c") && groupDef.getColumn_function().equals("month")){
                createElement("field", "Opportunity.Month_Close_Date__c", groupingsDownElement);
            }else if("Opportunities".equals(map.get("module")) && groupDef.getName().equals("date_closed") && groupDef.getColumn_function().equals("fiscalQuarter")){
                createElement("field", "Opportunity.Fiscal_Quarter_Expected_Closed_Date__c", groupingsDownElement);
            }else if("Opportunities".equals(map.get("module")) && groupDef.getName().equals("date_closed") && groupDef.getColumn_function().equals("week")){
                createElement("field", "Opportunity.Week_Expected_Close_Date__c", groupingsDownElement);
            }else if("Opportunities".equals(map.get("module")) && groupDef.getName().equals("date_closed") && groupDef.getColumn_function().equals("month")){
                createElement("field", "Opportunity.Month_Expected_Close_Date__c", groupingsDownElement);
            }else{
                createElement("field", rowMappingByKey.get(keyMap).get(1), groupingsDownElement);
            }
        }catch (Exception e){
            System.out.println(e.getMessage());
        }

        reportElement.appendChild(groupingsDownElement);
    }

    private static String getKeyMap(String col, String tableKey, String objectName){

        String keyMap = "";

        if(col.equals("date_entered") || col.equals("user_name")
                || col.equals("full_name") || col.equals("date_modified")){
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
            try {
                createElement("column", rowMappingByKey.get(keyMap).get(1), criteriaItemsElement);
            }catch (Exception e){
                System.out.println(e);
            }
        }
        createElement("columnToColumn", "false", criteriaItemsElement);
        createElement("isUnlocked", "true", criteriaItemsElement);
        String operator = convertOperator(filter.getQualifier_name());
        if(operator.equals("")){
            System.out.println("==> check filter: " + filter.getQualifier_name());
        }
        if("contains".equals(operator) && "Opportunity.ProductandServices__c".equals(rowMappingByKey.get(keyMap).get(1))){
            operator = "includes";
        }
        createElement("operator", operator, criteriaItemsElement);
        String value;
        if(filter.getInput_name0() instanceof  ArrayList){
            List<String> inputName0Tmp = new ArrayList<>();
            for(String inputName0 : (ArrayList<String>)filter.getInput_name0()){
                inputName0 = inputName0.replaceAll("1 Identified", "Interest");
                inputName0 = inputName0.replaceAll("2 Confirming", "Scoping");
                inputName0 = inputName0.replaceAll("3 Qualifying", "Scoping");
                inputName0 = inputName0.replaceAll("4 Proposing", "Proposal Sent");
                inputName0 = inputName0.replaceAll("5 Executing to Win", "Negotiation");
                inputName0 = inputName0.replaceAll("6 Contracting", "Contracting");
                inputName0 = inputName0.replaceAll("7 Closing", "Contracting");
                if(inputName0.equals("Dimension Tempo")){
                    inputName0 = "Service - DT - Dimension Tempo";
                }else if(inputName0.equals("Dimension SwifTest")){
                    inputName0 = "Product - DS - Dimension SwifTest";
                }else if(inputName0.equals("Dimension Focus")){
                    inputName0 = "Product - DF - Dimension Focus";
                }else if(inputName0.equals("Dimension LoadTest")){
                    inputName0 = "Product - DL - Dimension LoadTest";
                }
                inputName0Tmp.add(inputName0);
            }
            value = String.join(",", inputName0Tmp);


//            value = value.replaceAll("Dimension Focus and SwifTest", "Product - DF - Dimension Focus");
//            value = value.replaceAll("Dimension Products All", "Product - DF - Dimension Focus");
//            value = value.replaceAll("Dimension SwifTest and LoadTest", "Product - DF - Dimension Focus");

        }else{
            value = convertValue(filter.getInput_name0().toString(), filter.getQualifier_name());
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

    private static String convertValue(String inputValue, String operator) {
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
        }else if(inputValue.equals("not_empty") || inputValue.equals("empty")){
            returnString = "";
        }else if(inputValue.equals("tp_previous_fiscal_quarter")){
            returnString = "LAST FISCAL QUARTER";
        }else if(inputValue.equals("tp_current_fiscal_quarter")){
            returnString = "THIS FISCAL QUARTER";
        }else if(inputValue.equals("tp_next_fiscal_quarter")){
            returnString = "NEXT FISCAL QUARTER";
        }else if(operator.equals("tp_next_n_days")){
            returnString = "NEXT " + inputValue + " DAYS";
        }else if(operator.equals("tp_last_n_days")){
            returnString =  "LAST " + inputValue + " DAYS";
        }else if(inputValue.equals("tp_last_7_days")){
            returnString = "LAST 7 DAYS";
        }else if(inputValue.equals("tp_last_30_days")){
            returnString = "LAST 30 DAYS";
        }else if(inputValue.equals("tp_current_fiscal_year")){
            returnString = "THIS FISCAL YEAR";
        }else if(inputValue.equals("tp_previous_fiscal_year")){
            returnString = "LAST FISCAL YEAR";
        }else if(inputValue.equals("tp_last_month")){
            returnString = "LAST MONTH";
        }else if(inputValue.equals("tp_this_month")){
            returnString = "THIS MONTH";
        }
        return returnString;
    }
    private static String convertOperator(String inputOperator){
        String returnString = "";
        switch (inputOperator){
            case "after":
            case "greater":
                returnString = "greaterThan";
                break;
            case "one_of":
            case "contains":
                returnString = "contains";
                break;
            case "is":
            case "equals":
            case "empty":
            case "tp_previous_fiscal_quarter":
            case "tp_current_fiscal_quarter":
            case "tp_next_fiscal_quarter":
            case "tp_next_n_days":
            case "tp_last_n_days":
            case "tp_last_7_days":
            case "tp_last_30_days":
            case "tp_current_fiscal_year":
            case "tp_previous_fiscal_year":
            case "tp_last_month":
            case "tp_this_month":
                returnString = "equals";
                break;
            case "not_empty":
            case "is_not":
            case "not_equals":
                returnString = "notEqual";
                break;
            case "greaterOrEqual":
                returnString = "greaterOrEqual";
                break;
            case "lessOrEqual":
                returnString = "lessOrEqual";
                break;
            case "not_one_of":
            case "does_not_contain":
                returnString = "notContain";
                break;
            default:
                returnString = "";
        }
        return  returnString;
    }
    private static String convertName(String inputName){
        String returnString = "";
        try{
        switch (inputName){
            case "Opportunities":
                returnString = "Opportunity";
                break;
            case "Accounts":
                returnString = "Account";
                break;
            case "Contacts":
                returnString = "Contact";
                break;
            case "ss_Sales_Targets":
                // @TODO: remove when move to new sandbox. need to check "allow report" when create object
                returnString = "Sales_Target_Custom_Report__c";
                break;
            default:
                returnString = "CustomEntity$" + rowMappingByKey.get(inputName).get(1);
                break;
        }}catch (Exception e){
            System.out.println(e);
        }
        return  returnString;

    }
    private static void createElement(String tagName, String tagValue, Element parentElement){
        Element nameElement = doc.createElement(tagName);
        nameElement.setTextContent(tagValue);
        parentElement.appendChild(nameElement);
    }

    private static void createColumnElement(String columnValue, String aggregateTypes){
        Element columnElement = doc.createElement("columns");
        if(!aggregateTypes.isEmpty()){
            createElement("aggregateTypes",aggregateTypes, columnElement);
        }
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