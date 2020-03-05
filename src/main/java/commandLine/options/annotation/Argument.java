package commandLine.options.annotation;

import lombok.AllArgsConstructor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Argument {
    String UNDEFINED = "";
    String DEFAULT_SEPARATOR = ",";
    String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";

    String value()      default UNDEFINED; // this is opt

    String longOpt ()   default UNDEFINED;

    String description () default UNDEFINED;

    Type type () default Type.REQUIRED;

    @AllArgsConstructor
    enum Type {
        FLAG    (false, false,  "a flag that does not take value"),
        OPTIONAL(false, true,   "an optional key value argument"),
        REQUIRED(true,  true,   "an required key value argument");

        final boolean   isRequired;
        final boolean   hasArg;
        final String    defaultDescription;
    }
}
