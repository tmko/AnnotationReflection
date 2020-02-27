package commandLine.options.annotation;

public final class ParsingException extends RuntimeException {
    private ParsingException() { super(); }

    public static class IllDefinedOptions extends RuntimeException {
        public IllDefinedOptions() { super(); }
        public IllDefinedOptions(String msg) { super(msg); }
    }

    public static class IllegalState extends RuntimeException {
        public IllegalState() { super(); }
        public IllegalState(String msg) { super(msg); }
        public IllegalState(String msg, Exception e) { super(msg, e); }
    }

}