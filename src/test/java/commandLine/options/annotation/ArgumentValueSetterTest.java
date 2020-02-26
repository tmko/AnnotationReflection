package commandLine.options.annotation;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;

public class ArgumentValueSetterTest {

    @Test
    public void primitiveTest () {

        class Arg extends ArgumentParser {
            @Argument("b") boolean aBoolean;
            @Argument("i") int anInt;
            @Argument("y") byte aByte;
            @Argument("c") char aChar;
            @Argument("s") short aShort;
            @Argument("l") long aLong;
            @Argument("f") float aFloat;
            @Argument("d") double aDouble;
        }

        Arg holder = new Arg();
        holder.parse("-b","false","-i","10","-y","1","-c","z","-s","3","-l","1000","-f","0.3","-d", "0.1");

        Assert.assertThat(holder.aBoolean,  is(false));
        Assert.assertThat(holder.anInt,     is(10));
        Assert.assertThat(holder.aByte,     is(Byte.valueOf("1")));
        Assert.assertThat(holder.aChar,     is('z'));
        Assert.assertThat(holder.aShort,    is(Short.valueOf("3")));
        Assert.assertThat(holder.aLong,     is(1000l));
        Assert.assertThat(holder.aFloat,    is(0.3f));
        Assert.assertThat(holder.aDouble,   is(0.1d));
    }

    @Test
    public void wrapperTest () {

        class Arg extends ArgumentParser {
            @Argument("b") Boolean aBoolean;
            @Argument("i") Integer anInt;
            @Argument("y") Byte aByte;
            @Argument("c") Character aChar;
            @Argument("s") Short aShort;
            @Argument("l") Long aLong;
            @Argument("f") Float aFloat;
            @Argument("d") Double aDouble;
        }

        Arg holder = new Arg();
        holder.parse("-b","false","-i","10","-y","1","-c","z","-s","3","-l","1000","-f","0.3","-d", "0.1");

        Assert.assertThat(holder.aBoolean,  is(false));
        Assert.assertThat(holder.anInt,     is(10));
        Assert.assertThat(holder.aByte,     is(Byte.valueOf("1")));
        Assert.assertThat(holder.aChar,     is('z'));
        Assert.assertThat(holder.aShort,    is(Short.valueOf("3")));
        Assert.assertThat(holder.aLong,     is(1000l));
        Assert.assertThat(holder.aFloat,    is(0.3f));
        Assert.assertThat(holder.aDouble,   is(0.1d));
    }

    @Test
    public void objectTest () throws ParseException {

        class Arg extends ArgumentParser {
            @Argument("s") String aString;
            @Argument("d") Date aDate;
            @Argument("f") File aFile;
            @Argument("p") Path aPath;
        }

        Arg holder = new Arg();
        holder.parse("-s","some value","-d","2020-01-22","-f","~/somedir/./../somefile.ext","-p","/a/33/");

        Assert.assertThat(holder.aString,   is("some value"));
        Assert.assertThat(holder.aDate,     is(new SimpleDateFormat("yyyy-MM-dd").parse("2020-01-22")));
        Assert.assertThat(holder.aFile,     is(new File("~/somedir/./../somefile.ext")));
        Assert.assertThat(holder.aPath,     is(Paths.get("/a/33/")));
    }


    @Test
    public void arrayTest ()  {

        class Arg extends ArgumentParser {
            @Argument("bool") Boolean[] aBoolean;
            @Argument("int") Integer[] anInt;
            @Argument("byte") Byte[] aByte;
            @Argument("char") Character[] aChar;
            @Argument("short") Short[] aShort;
            @Argument("long") Long[] aLong;
            @Argument("float") Float[] aFloat;
            @Argument("double") Double[] aDouble;
            @Argument("str") String[] aString;
        }

        Arg holder = new Arg();
        holder.parse(
                "-int","1,2, 3", "-bool","true, false", "-byte", "1,0", "-char", "a, b",
                "-short", " 1,2", "-long", "1", "-float", "0.1", "-double", "0.2",
                "-str", "str1, str2"
                );

        Assert.assertThat(holder.anInt,     is(new Integer[]{1,2,3}));
        Assert.assertThat(holder.aBoolean,  is(new Boolean[]{true,false}));
        Assert.assertThat(holder.aByte,     is(new Byte[]{Byte.valueOf("1"), Byte.valueOf("0")}));
        Assert.assertThat(holder.aChar,     is(new Character[]{'a','b'}));
        Assert.assertThat(holder.aString,   is(new String[]{"str1", "str2"}));
    }

    @Test
    public void objectArrayTest () throws ParseException {

        class Arg extends ArgumentParser {
            @Argument("d") Date[] aDate;
            @Argument("f") File[] aFile;
            @Argument("p") Path[] aPath;
        }

        Arg holder = new Arg();
        holder.parse(
                "-d","2020-01-22, 2020-02-22",
                "-f","~/somedir/, ~/somefile.ext",
                "-p","/a/33/");

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date[] dates = new Date[] { format.parse("2020-01-22"), format.parse("2020-02-22")};
        File[] files = new File[] { new File("~/somedir/"), new File("~/somefile.ext")};

        Assert.assertThat(holder.aDate,     is(dates));
        Assert.assertThat(holder.aFile,     is(files));
        Assert.assertThat(holder.aPath[0],  is(Paths.get("/a/33/")));
    }


    @Test
    public void emptyArrayTest () throws ParseException {

        class Arg extends ArgumentParser {
            @Argument("int") Integer[] anInt;
        }

        Arg holder = new Arg();
        holder.parse("-int", "");

        Assert.assertThat(holder.anInt.length,  is(0));
    }
}
