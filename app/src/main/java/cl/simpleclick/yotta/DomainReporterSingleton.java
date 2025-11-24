package cl.simpleclick.yotta;

public class DomainReporterSingleton {
    private static DomainReporter reporter;

    public static void setReporter(DomainReporter r) {
        reporter = r;
    }

    public static DomainReporter getReporter() {
        return reporter;
    }
}