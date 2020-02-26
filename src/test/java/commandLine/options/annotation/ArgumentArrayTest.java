package commandLine.options.annotation;

import org.junit.Test;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ArgumentArrayTest {
    String SEPARATOR = ",";

    Integer[] integerArray;


    private Map<Class<?>, Function<String, ?>> fieldsValueSetters = new HashMap<>();

    @Test
    public void integerArrayTest () throws NoSuchFieldException, IllegalAccessException {

        Field f = this.getClass().getDeclaredField("integerArray");
        Class c = Integer[].class;
        fieldsValueSetters.put(c, toArray(Integer.class, Integer::valueOf));

        Object converted = fieldsValueSetters.get(c).apply("1,2,3,4,5,6");
        f.set(this, converted);

        System.out.println(Arrays.toString(integerArray) );
    }



    public <T> Function<String, Object> toArray (Class<T> type, Function<String, T> converter) {
        return (stringWithSeparator) -> {
            String[] values = stringWithSeparator.split(SEPARATOR);
            Object array = Array.newInstance(type, values.length);
            for (int i=0; i<values.length; i++) {
                String value = values[i];
                Object converted = converter.apply(value);
                Array.set(array, i, type.cast(converted));
            }
            return array;
        };
    }



}
