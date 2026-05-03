package ba.nwt.bookingservice.exception;

public class DownstreamBadRequestException extends RuntimeException {
    private final String service;
    private final int status;

    public DownstreamBadRequestException(String service, int status, String message) {
        super(message);
        this.service = service;
        this.status = status;
    }

    public String getService() {
        return service;
    }

    public int getStatus() {
        return status;
    }
}
