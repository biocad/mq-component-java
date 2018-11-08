package component;

import exception.InvalidValueException;
import protocol.MoniqueMessage;

import static component.Constant.DELIMETER;


/**
 * Utility class
 * Provide methods to work with MoniQue message tags
 *
 * @author Pavel Didkovskii
 */
public class TagUtils {

    /**
     * Extract message tag from MoniQue message
     *
     * @param moniqueMessage
     * @return message tag
     */
    static String createMessageTag(MoniqueMessage moniqueMessage) {
        return String.join(DELIMETER,
                moniqueMessage.getType(),
                moniqueMessage.getSpec(),
                moniqueMessage.getId(),
                moniqueMessage.getPid(),
                moniqueMessage.getCreator());
    }

    /**
     * Extract tag part from MoniQue message tag
     *
     * @param tag     - MoniQue message tag
     * @param tagPart
     * @return message tag
     */
    public static String getTagPart(String tag, TagPart tagPart) throws InvalidValueException {
        return getTagPart(tag, tagPart.key);
    }

    private static String getTagPart(String tag, int pos) throws InvalidValueException {
        if (pos < 0 || pos > 4) {
            throw new InvalidValueException("Tag field out of bounds");
        }
        String[] tagSplit = tag.split(DELIMETER);
        if (tagSplit.length != 5) {
            throw new InvalidValueException("Not a message tag!");
        }
        return tagSplit[pos];
    }

    public enum TagPart {

        TYPE(0), SPEC(1), ID(2), PID(3), CREATOR(4);

        private int key;

        TagPart(int key) {
            this.key = key;
        }
    }

}
