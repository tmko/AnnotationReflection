package commandLine.options.annotation;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.core.Is.is;

public class ArgumentInheritanceTest extends DummyParent {

    @Argument("c")
    protected String childVar;


    @Test
    public void ArgumentDiscoveryTest () {
        ArgumentInheritanceTest var = new ArgumentInheritanceTest();
        var.parse("-p", "pValue", "-c", "cValue");
        Assert.assertThat(var.parentVar, is("pValue"));
        Assert.assertThat(var.childVar,  is("cValue"));
    }
}
