package protocol;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Monique message with tag
 *
 * @author Pavel Didkovskii
 * */
@Getter
@AllArgsConstructor
public class MoniqueTaggedMessage {

    private String tag;

    private MoniqueMessage moniqueMessage;

}
