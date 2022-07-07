public class WrapInt{
    public Integer value;
    WrapInt(Integer value)
    {

        // Using wrapper class
        // so as to wrap integer value
        // in mutable object
        // which can be changed or modified
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    @Override
    public String toString()
    {
        return String.valueOf(value);
    }
}