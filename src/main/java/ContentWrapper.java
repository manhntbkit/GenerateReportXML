import java.util.ArrayList;
import java.util.Map;

public class ContentWrapper {
    ArrayList < ColumnObj > display_columns = new ArrayList< ColumnObj >();
    FiltersDef filters_def = new FiltersDef();

    class FiltersDef{
        Map<String, Object> Filter_1;
    }

    static class Filter{
        String name;
        String table_key;
        String qualifier_name;
        Object input_name0;
        String runtime;
        String input_name1;
        String operator;

        public String getOperator() {
            return operator;
        }

        public void setOperator(String operator) {
            this.operator = operator;
        }

        public Filter(){};
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getTable_key() {
            return table_key;
        }

        public void setTable_key(String table_key) {
            this.table_key = table_key;
        }

        public String getQualifier_name() {
            return qualifier_name;
        }

        public void setQualifier_name(String qualifier_name) {
            this.qualifier_name = qualifier_name;
        }

        public Object getInput_name0() {
            return input_name0;
        }

        public void setInput_name0(Object input_name0) {
            this.input_name0 = input_name0;
        }

        public String getRuntime() {
            return runtime;
        }

        public void setRuntime(String runtime) {
            this.runtime = runtime;
        }

        public String getInput_name1() {
            return input_name1;
        }

        public void setInput_name1(String input_name1) {
            this.input_name1 = input_name1;
        }
    }
    class ColumnObj {
        private String name;
        private String label;
        private String table_key;

        // Getter Methods

        public String getName() {
            return name;
        }

        public String getLabel() {
            return label;
        }

        public String getTable_key() {
            return table_key;
        }

        // Setter Methods
        public void setName(String name) {
            this.name = name;
        }
        public void setLabel(String label) {
            this.label = label;
        }
        public void setTable_key(String table_key) {
            this.table_key = table_key;
        }
    }
}
