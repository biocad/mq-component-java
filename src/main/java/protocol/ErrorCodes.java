package protocol;

/**
 * MoniQue error codes
 *
 * @author Pavel Didkovskii
 */
public enum ErrorCodes {

    PROTOCOL_ERROR(100),
    ENCODING_ERROR(101),
    TRANSPORT_ERROR(200),
    TAG_ERROR(201),
    TECHNICAL_ERROR(300),
    KILLED_ERROR(301),
    COMPONENT_ERROR(500),
    FOREIGN_ERROR(501);

    private int code;

    public int getCode() {
        return code;
    }

    ErrorCodes(int code) {
        this.code = code;
    }
}
