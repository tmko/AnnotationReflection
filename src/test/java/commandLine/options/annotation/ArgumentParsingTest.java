package commandLine.options.annotation;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.core.Is.is;

public class ArgumentParsingTest {

    @Test
    public void noAnnotationTest() {
        class Arg extends ArgumentParser {
            String param = "value should not change";
        }

        Arg holder = new Arg();
        holder.parse("");

        Assert.assertThat(holder.param, is("value should not change"));
    }

    @Test
    public void simpleTest () {
        class Arg extends ArgumentParser {
            @Argument("r1")                                        String required1 = "value should change";
            @Argument(value = "r2", type = Argument.Type.REQUIRED) String required2 = "value should change";
            @Argument(value = "o1", type = Argument.Type.OPTIONAL) String optionalunset = "value should not change";
            @Argument(value = "o2", type = Argument.Type.OPTIONAL) String optionalset   = "value should change";
            @Argument(value = "a",  type = Argument.Type.FLAG)  boolean flag1 = true;
            @Argument(value = "b",  type = Argument.Type.FLAG)  boolean flag2 = false;
            String someOtherField = "some value";
        }

        Arg holder = new Arg();
        holder.parse("-r1", "value1", "-r2", "value2", "-o2", "someOtherValue", "-b");

        Assert.assertThat(holder.required1, is("value1"));
        Assert.assertThat(holder.required2, is("value2"));
        Assert.assertThat(holder.optionalunset, is("value should not change"));
        Assert.assertThat(holder.optionalset, is("someOtherValue"));
        Assert.assertThat(holder.someOtherField, is("some value"));
        Assert.assertFalse(holder.flag1);
        Assert.assertTrue(holder.flag2);
    }

    @Test
    public void setFinalVariableTest () {
        class Arg extends ArgumentParser {
            @Argument("r") private String required1 = "value should change";
        }

        Arg holder = new Arg();
        holder.parse("-r", "value1");

        Assert.assertThat(holder.required1, is("value1"));
    }

    @Test (expected = ParsingException.IllegalState.class)
    public void missingKeyValueInputFromArguments () throws Exception {
        class CmdLineArgsValueHolder extends ArgumentParser {
            @Argument(value = "s", description = "desc")
            private final String param = null;
        }

        CmdLineArgsValueHolder holder = new CmdLineArgsValueHolder();
        holder.parse("-m", "value for m");
    }

    @Test (expected = ParsingException.IllegalState.class)
    public void unsupportedInputFromArguments() {
        class CmdLineArgsValueHolder extends ArgumentParser {
            @Argument(value = "s", type = Argument.Type.OPTIONAL)
            private final String param = null;
        }

        CmdLineArgsValueHolder holder = new CmdLineArgsValueHolder();
        holder.parse("-m", "value for m");
    }


}
