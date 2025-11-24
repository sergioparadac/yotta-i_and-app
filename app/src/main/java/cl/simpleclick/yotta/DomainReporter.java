package cl.simpleclick.yotta;


public interface DomainReporter {
    enum Source { DNS, TLS_SNI }
    void onDomainObserved(String domain, Source source);
}

class LogDomainReporter implements DomainReporter {
    @Override
    public void onDomainObserved(String domain, Source source) {
        android.util.Log.i("DomainObserver", source + " -> " + domain);
    }
}