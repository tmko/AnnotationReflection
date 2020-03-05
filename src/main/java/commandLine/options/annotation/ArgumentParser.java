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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

@Slf4j
public class ArgumentParser {

    private static final String NOT_ARGUMENT_TYPE = "%s is not an Argument annotation.";
    private static final String MISSING_OPT = "%s does not define a flag.";
    private static final String WRAPPER_FOR_OPTIONAL = "%s is a primitive and optional. If missing, can't set to null";
    private static final String FLAG_MUST_BE_BOOL = "%s does not take value, so it is a flag and must be boolean.";
    private static final String UNEXPECTED_ARGUMENT_TYPE = "A new Argument type is detected but not handle.";
    private static final String ERROR_PARSING_ARG = "Error found during argument parsing %s";
    private static final String PRIMITIVE_ARRAY_FOUND = "Use wrapper type for array for field %s";
    private static final String OPTION_DEFINED ="Option field {} defined opt:{},{} hasArgs:{}, required:{}.";

    private Options options = new Options();
    private Map<Class<?>, Function<String, ?>> stringValueTransformer = new HashMap<>();

    private List<Field> fieldsAnnotatedAsArgument;

    public ArgumentParser() {
        this.fieldsAnnotatedAsArgument = getArgumentAnnotationsThroughInheritance();

        for (Field field : fieldsAnnotatedAsArgument) {
            Argument annotation = field.getAnnotation(Argument.class);

            validate(field.getName(), field.getType(), annotation);
            Option optionWithBasicSetting = cliCommandOptionObjectFactory(annotation);
            Optional<Option> option = hookForCustomOptionSetting(field, optionWithBasicSetting);
            option.ifPresent(x -> options.addOption(x));
        }
    }

    private Option cliCommandOptionObjectFactory(Argument annotation) {
        boolean isLongOptionDefined = defined(annotation.longOpt());
        boolean isDescriptionProvided = defined(annotation.description());
        boolean isRequired = annotation.type().isRequired;
        boolean hasArg = annotation.type().hasArg;
        String opt = annotation.value();
        String description = isDescriptionProvided ? annotation.description() : annotation.type().defaultDescription;

        Option option = new Option(opt, hasArg, description);
        option.setRequired(isRequired);
        if ( isLongOptionDefined ) {
            option.setLongOpt(annotation.longOpt());
        }

        return option;
    }

    protected Optional<Option> hookForCustomOptionSetting (Field f, Option o) {
        log.info(OPTION_DEFINED, f.getName(), o.getOpt(), o.hasLongOpt(), o.hasArgs(), o.isRequired());
        return Optional.of(o);
    }

    /**
     * This method assign value to the instance variables
     * @param args The String array in main()
     * @return this method return this for linking method call
     */
    public ArgumentParser parse (String... args) {
        configureDefaultFieldsValueSetters();
        stringValueTransformer.putAll(hookCustomTypeConversion());

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            for (Field field : this.fieldsAnnotatedAsArgument){
                Argument annotation = field.getAnnotation(Argument.class);
                String value = getCmdValueFromTerminalAsString(annotation, cmd);
                setValue(field, value);
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
        switch (annotation.type()) {
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
        if ( value == null )
            return;

        boolean wasAccessible = f.isAccessible();
        f.setAccessible(true);
        try {
            value = value.trim();
            Class c = f.getType();
            Function<String, ?> func = stringValueTransformer.get(c);
            Object converted = func.apply(value);
            f.set(this, converted);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            f.setAccessible(wasAccessible);
        }
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

    public List<Field> getArgumentAnnotationsThroughInheritance() {
        ArrayList<Field> result = new ArrayList<>();
        Class obj = getClass();
        do {
            Field[] fields =obj.getDeclaredFields();
            Arrays.asList(fields).stream().filter(ArgumentParser::isArgument).forEach(result::add);
            obj = obj.getSuperclass();
        } while ( Objects.nonNull(obj) );
        return result;
    }

    public void configureDefaultFieldsValueSetters () {
        stringValueTransformer.put(boolean.class, Boolean::valueOf);
        stringValueTransformer.put(int.class,     Integer::valueOf);
        stringValueTransformer.put(byte.class,    Byte::valueOf);
        stringValueTransformer.put(char.class,    x -> x.charAt(0));
        stringValueTransformer.put(short.class,   Short::valueOf);
        stringValueTransformer.put(long.class,    Long::valueOf);
        stringValueTransformer.put(float.class,   Float::valueOf);
        stringValueTransformer.put(double.class,  Double::valueOf);

        stringValueTransformer.put(Boolean.class,    Boolean::valueOf);
        stringValueTransformer.put(Integer.class,    Integer::valueOf);
        stringValueTransformer.put(Byte.class,       Byte::valueOf);
        stringValueTransformer.put(Character.class,  x -> x.charAt(0));
        stringValueTransformer.put(Short.class,      Short::valueOf);
        stringValueTransformer.put(Long.class,       Long::valueOf);
        stringValueTransformer.put(Float.class,      Float::valueOf);
        stringValueTransformer.put(Double.class,     Double::valueOf);

        stringValueTransformer.put(Boolean[].class,    toArray(Boolean.class, Boolean::valueOf));
        stringValueTransformer.put(Integer[].class,    toArray(Integer.class, Integer::valueOf));
        stringValueTransformer.put(Byte[].class,       toArray(Byte.class,    Byte::valueOf));
        stringValueTransformer.put(Character[].class,  toArray(Character.class, x -> x.charAt(0)));
        stringValueTransformer.put(Short[].class,      toArray(Short.class, Short::valueOf));
        stringValueTransformer.put(Long[].class,       toArray(Long.class,  Long::valueOf));
        stringValueTransformer.put(Float[].class,      toArray(Float.class, Float::valueOf));
        stringValueTransformer.put(Double[].class,     toArray(Double.class, Double::valueOf));

        stringValueTransformer.put(String.class,    x -> x);
        stringValueTransformer.put(Date.class,      ArgumentParser::toDate);
        stringValueTransformer.put(File.class,      File::new);
        stringValueTransformer.put(Path.class,      Paths::get);

        stringValueTransformer.put(String[].class,    toArray(String.class, x->x));
        stringValueTransformer.put(Date[].class,      toArray(Date.class, ArgumentParser::toDate));
        stringValueTransformer.put(File[].class,      toArray(File.class, File::new));
        stringValueTransformer.put(Path[].class,      toArray(Path.class, Paths::get));
    }

    public <T> Function<String, Object> toArray (Class<T> type, Function<String, T> parseAsFunction) {
        return (stringWithSeparator) -> {
            String[] inputValues = stringWithSeparator.split(Argument.DEFAULT_SEPARATOR);
            List<String> inputValuesTrimmed = Arrays.asList(inputValues).stream()
                    .filter(ArgumentParser::defined)
                    .map(String::trim)
                    .collect(toList());

            T[] resultArray = (T[])Array.newInstance(type, inputValuesTrimmed.size());
            Arrays.setAll(resultArray, index -> {
                String str = inputValuesTrimmed.get(index);
                T convertedValue = (T)parseAsFunction.apply(str);
                return convertedValue;
            });
            return resultArray;
        };
    }


    private void check (boolean isOK, String exceptionMsg) {
        if (!isOK) throw new ParsingException.IllDefinedOptions(exceptionMsg);
    }

    private static boolean isArgument (Field f) {
        return Objects.nonNull(f.getAnnotation(Argument.class));
    }

    private static boolean defined (String s) {
        boolean undefine = Objects.isNull(s) || s.isEmpty() || Argument.UNDEFINED.equals(s);
        return !undefine;
    }

    @SneakyThrows
    private static Date toDate (String s) {
        return new SimpleDateFormat (Argument.DEFAULT_DATE_FORMAT).parse(s);
    }

}
