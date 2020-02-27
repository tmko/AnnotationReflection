package commandLine.options.annotation;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class ArgumentParser {
    public final static String SEPARATOR = ",";
    public final static String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    public final static Map<Class<?>, Function<String, ?>> NO_CUSTOM_SETTER = Collections.EMPTY_MAP;

    private static final String NOT_ARGUMENT_TYPE = "%s is not an Argument annotation.";
    private static final String MISSING_OPT = "%s does not define a flag.";
    private static final String WRAPPER_FOR_OPTIONAL = "%s is a primitive and optional. If missing, can't set to null";
    private static final String FLAG_MUST_BE_BOOL = "%s does not take value, so it is a flag and must be boolean.";
    private static final String UNEXPECTED_ARGUMENT_TYPE = "A new Argument type is detected but not handle.";
    private static final String ERROR_PARSING_ARG = "Error found during argument parsing %s";
    private static final String PRIMITIVE_ARRAY_FOUND = "Use wrapper type for array for field %s";
    private static final String OPTION_DEFINED ="Option field %s defined opt:%s,%s hasArgs:%b, required:%b ";

    private Options options = new Options();

    private List<Field> fieldsAnnotatedAsArgument = Collections.EMPTY_LIST;
    private Map<Class<?>, Function<String, ?>> fieldsValueSetters = new HashMap<>();

    public ArgumentParser() {
        Class<?> childClass = getClass();
        List<Field> allFields = Arrays.asList(childClass.getDeclaredFields());
        this.fieldsAnnotatedAsArgument = allFields.stream()
                .filter(f -> f.getAnnotation(Argument.class) != null)
                .collect(Collectors.toList());

        for (Field field : fieldsAnnotatedAsArgument) {
            Argument annotation = field.getAnnotation(Argument.class);

            validate(field.getName(), field.getType(), annotation);
            Option optionWithBasicSetting = objectFactory(annotation);
            Optional<Option> option = hookForCustomOptionSetting(field, optionWithBasicSetting);
            option.ifPresent(x -> options.addOption(x));
        }
    }

    protected Optional<Option> hookForCustomOptionSetting (Field f, Option o) {
        log.info(String.format(OPTION_DEFINED, f.getName(), o.getOpt(), o.hasLongOpt(), o.hasArgs(), o.isRequired()));
        return Optional.of(o);
    }


    private Option objectFactory(Argument annotation) {
        boolean isLongOptionDefined = defined(annotation.longOpt());
        boolean isDescriptionProvided = defined(annotation.description());

        boolean hasArg = annotation.type().hasArg;
        String opt = annotation.value();
        String description = isDescriptionProvided ? annotation.description() : annotation.type().defaultDescription;

        Option option = new Option(opt, hasArg, description);
        option.setRequired(annotation.type().isRequired);
        if ( isLongOptionDefined ) {
            option.setLongOpt(annotation.longOpt());
        }

        return option;
    }

    /**
     * This method assign value to the instance variables
     * @param args The String array in main()
     * @return this method return this for linking method call
     */
    public ArgumentParser parse (String... args) {
        configureDefaultFieldsValueSetters();
        fieldsValueSetters.putAll(hookCustomTypeConversion());

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            for (Field field : this.fieldsAnnotatedAsArgument){
                Argument annotation = field.getAnnotation(Argument.class);
                String value = getCmdValueFromTerminalAsString(annotation, cmd);
                if ( Objects.nonNull(value) ) {
                    setValue(field, value.trim());
                }
            }
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(e.getMessage(), options);
            throw new ParsingException.IllegalState(String.format(ERROR_PARSING_ARG, Arrays.toString(args)), e);
        }
        return this;
    }

    public Map<Class<?>, Function<String, ?>> hookCustomTypeConversion (){
        return Collections.emptyMap();
    }

    public String getCmdValueFromTerminalAsString(Argument annotation, CommandLine cmd) {
        String opt = annotation.value();
        Argument.Type type = annotation.type();
        switch (type) {
            case REQUIRED:
                return cmd.getOptionValue(opt);
            case OPTIONAL:
                return cmd.hasOption(opt) ? cmd.getOptionValue(opt) : null;
            case FLAG:
                return cmd.hasOption(opt) + "";
            default:
                throw new ParsingException.IllegalState(UNEXPECTED_ARGUMENT_TYPE);
        }
    }

    protected void setValue(Field f, String value) {
        boolean wasAccessible = f.isAccessible();
        f.setAccessible(true);
        try {
            Class c = f.getType();
            Function<String, ?> func = fieldsValueSetters.get(c);
            Object converted = func.apply(value);
            f.set(this, converted);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            f.setAccessible(wasAccessible);
        }
    }





    private void configureDefaultFieldsValueSetters () {
        setFieldsValueSetters(boolean.class, Boolean.class,    Boolean::valueOf);
        setFieldsValueSetters(int.class,     Integer.class,    Integer::valueOf);
        setFieldsValueSetters(byte.class,    Byte.class,       Byte::valueOf);
        setFieldsValueSetters(char.class,    Character.class,  x -> x.charAt(0));
        setFieldsValueSetters(short.class,   Short.class,      Short::valueOf);
        setFieldsValueSetters(long.class,    Long.class,       Long::valueOf);
        setFieldsValueSetters(float.class,   Float.class,      Float::valueOf);
        setFieldsValueSetters(double.class,  Double.class,     Double::valueOf);

        setFieldsValueSetters(String.class, x -> x);
        setFieldsValueSetters(Date.class,   ArgumentParser::toDate);
        setFieldsValueSetters(File.class,   File::new);
        setFieldsValueSetters(Path.class,   Paths::get);
    }


    public <T> void setFieldsValueSetters (Class primitive, Class wrapper, Function<String, T> converter) {
        setFieldsValueSetters (wrapper, converter);
        fieldsValueSetters.put(primitive, converter);
    }

    public <T> void setFieldsValueSetters (Class type, Function<String, T> converter) {
        Class array = Array.newInstance(type, 0).getClass();
        fieldsValueSetters.put(type,   converter);
        fieldsValueSetters.put(array,  toArray(type, converter));
    }

    public <T> Function<String, Object> toArray (Class<T> type, Function<String, T> converter) {
        return (stringWithSeparator) -> {

            List<String> values = Arrays.asList(stringWithSeparator.split(SEPARATOR))
                    .stream()
                    .filter(this::defined)
                    .map(String::trim)
                    .collect(Collectors.toList());

            Object array = Array.newInstance(type, values.size());
            for (int i=0; i<values.size(); i++) {
                Object converted = converter.apply(values.get(i));
                Array.set(array, i, type.cast(converted));
            }
            return array;
        };
    }





    private void validate (String fieldName, Class fieldType, Argument annotation) {
        check(Objects.nonNull(annotation),  String.format(NOT_ARGUMENT_TYPE, fieldName));
        check(defined(annotation.value()),  String.format(MISSING_OPT, fieldName));

        if ( fieldType.isArray() ) {
            check(!fieldType.isPrimitive(), String.format(PRIMITIVE_ARRAY_FOUND, fieldName));
        }

        if ( annotation.type() == Argument.Type.OPTIONAL ) {
            check(!fieldType.isPrimitive(), String.format(WRAPPER_FOR_OPTIONAL, fieldName));
        }

        if ( annotation.type() == Argument.Type.FLAG ) {
            boolean isBoolean = boolean.class.equals(fieldType) || Boolean.class.equals(fieldType);
            check(isBoolean,    String.format(FLAG_MUST_BE_BOOL, fieldName));
        }
    }

    private void check (boolean isOK, String exceptionMsg) {
        if (!isOK) {
            throw new ParsingException.IllDefinedOptions(exceptionMsg);
        }
    }

    private boolean defined (String s) {
        return Objects.nonNull(s) && !s.isEmpty() && !Argument.UNDEFINED.equals(s);
    }

    @SneakyThrows
    private static Date toDate (String s) {
        return new SimpleDateFormat (DEFAULT_DATE_FORMAT).parse(s);
    }


}
