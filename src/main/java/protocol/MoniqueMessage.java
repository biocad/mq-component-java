package protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.codec.digest.DigestUtils;
import sun.misc.BASE64Encoder;

import java.util.Random;

import static component.Constant.DELIMETER;


/**
 * Monique message contains data for processing by MoniQue core functionality
 * It is packed into MessagePack format and unpacked from it
 *
 * @author Pavel Didkovskii
 */
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class MoniqueMessage {
    private final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

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
        this.id = assignMessageId();
    }

    private String assignMessageId() {
        StringBuilder idBuilder = new StringBuilder();
        Random rand = new Random();
        char[] chars = alphabet.toCharArray();
        
        for (int i = 0; i < 40; i++) {
            int randomIndex = rand.nextInt(alphabet.length());
            idBuilder.append(chars[randomIndex]);
        }
        return idBuilder.toString();
    }

}
