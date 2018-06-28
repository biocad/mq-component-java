package protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.codec.digest.DigestUtils;
import sun.misc.BASE64Encoder;

import static component.Constant.DELIMETER;


/**
 * Monique message contains data for processing by MoniQue core functionality
 * It is packed into MessagePack format and unpacked from it
 *
 * @author Pavel Didkovskii
 * */
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class MoniqueMessage {

    private String id;

    private String pid;

    private String creator;

    @JsonProperty("created_at")
    private Integer createdAt;

    @JsonProperty("expires_at")
    private Integer expiresAt;

    private String spec;

    private String encoding;

    private String type;

    private byte[] data;

    public MoniqueMessage(String pid, String creator, Integer expiresAt, String spec, String encoding, String type, byte[] data) {
        this.pid = pid;
        this.creator = creator;
        this.createdAt = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
        this.expiresAt = expiresAt;
        this.spec = spec;
        this.encoding = encoding;
        this.type = type;
        this.data = data;
        this.id = assignMessageId(this);
    }

    private String assignMessageId(MoniqueMessage message) {
        return new BASE64Encoder().encode(DigestUtils.sha1(
                String.join(DELIMETER, message.getCreator(), String.valueOf(message.getCreatedAt()),
                        message.getSpec())));
    }

}
