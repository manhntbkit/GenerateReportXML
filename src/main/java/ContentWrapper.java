import java.util.ArrayList;

public class ContentWrapper {
    ArrayList < ColumnObj > display_columns = new ArrayList< ColumnObj >();

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
