import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReadCSV {
//    public static void main(String[] args)
//            throws ParserConfigurationException, TransformerException, IOException {
//        readFile();
//    }
    public static List<List<String>>  readFile(){
        List<List<String>> records = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("src//data//csv//sugarToSalesforce.csv"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if(!"SugarCRM".equals(values[0])){
                    records.add(Arrays.asList(values));
                }

            }
        } catch(Exception ex){
            System.out.println(ex.getCause() + ex.getMessage());
        }
        return records;
    }

}
