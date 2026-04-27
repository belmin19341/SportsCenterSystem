package ba.nwt.bookingservice.exception;

public class DownstreamUnavailableException extends RuntimeException {
    private final String service;

    public DownstreamUnavailableException(String service, String message) {
        super(message);
        this.service = service;
    }

    public DownstreamUnavailableException(String service, String message, Throwable cause) {
        super(message, cause);
        this.service = service;
    }

    public String getService() {
        return service;
    }
}
