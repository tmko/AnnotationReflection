package commandLine.options.annotation;


import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.core.IsInstanceOf.instanceOf;


public class ArgumentTypeTest {

    @Test
    public void simpleArgumentTypeTest() {
        class Arg extends ArgumentParser {
            @Argument(value = "a", type = Argument.Type.OPTIONAL) Integer aInteger;
            @Argument(value = "b", type = Argument.Type.REQUIRED) int anInt;
            @Argument(value = "c", type = Argument.Type.FLAG) Boolean aBoolean;
            @Argument(value = "d", type = Argument.Type.FLAG) boolean aBool;
            @Argument("e") String aString;
        }

        Assert.assertNotNull(new Arg());
    }


    @Test
    public void optionalWithPrimitiveArguments() {
        //When
        class Arg extends ArgumentParser {
            @Argument(value = "s", type = Argument.Type.OPTIONAL)
            int param;
        }

        //Given
        try { new Arg(); }

        //Verify
        catch (Throwable e) {
            Assert.assertThat(e, instanceOf(ParsingException.IllDefinedOptions.class));
            Assert.assertTrue(e.getMessage().contains("primitive") );
        }
    }

    @Test
    public void optionalWithPrimitiveArrayArguments() {
        //When
        class Arg extends ArgumentParser {
            @Argument(value = "s", type = Argument.Type.OPTIONAL)
            byte[] param;
        }

        //Given
        try { new Arg(); }

        //Verify
        catch (Throwable e) {
            Assert.assertThat(e, instanceOf(ParsingException.IllDefinedOptions.class));
            Assert.assertTrue(e.getMessage().contains("primitive") );
        }
    }

    @Test
    public void nonBooleanFlagArguments() {
        //When
        class Arg extends ArgumentParser {
            @Argument(value = "s", type = Argument.Type.FLAG)
            Integer param;
        }

        //Given
        try { new Arg(); }

        //Verify
        catch (Throwable e) {
            Assert.assertThat(e, instanceOf(ParsingException.IllDefinedOptions.class));
            Assert.assertTrue(e.getMessage().contains("boolean") );
        }
    }

}
