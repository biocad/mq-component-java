package component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.msgpack.value.Variable;

import java.io.IOException;

/**
* Utility class
* Provides methods to convert different types of data
*
* @author Pavel Didkovskii
*/
public class Converter {

    public static byte[] objectToByteArray(Object o) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsBytes(o);
    }

    public static <T> T objectFromByteArray(byte[] data, Class<T> clazz) throws IOException {
        return new ObjectMapper().readValue(data, clazz);
    }

    /**
     * @param o - object for serialization into MessagePack byte array
     * @return byte array
     * @throws IOException
     */
    static byte[] objectToMessagePack(Object o) throws IOException {
        if (o instanceof String) {
            return stringToMessagePack((String) o);
        }
        return new ObjectMapper(new MessagePackFactory()).writeValueAsBytes(o);
    }

    /**
     * @param data - byte array data for MessagePack byte array deserialization
     * @return - deserialized object
     * @throws IOException
     */
    static <T> T objectFromMessagePack(byte[] data, Class<T> clazz) throws IOException {
        if (clazz == String.class) {
            return (T) stringFromMessagePack(data);
        }
        return new ObjectMapper(new MessagePackFactory()).readValue(data, clazz);
    }

    private static String stringFromMessagePack(byte[] data) throws IOException {
        return MessagePack.newDefaultUnpacker(data).unpackString();
    }

    private static byte[] stringToMessagePack(String data) throws IOException {
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        packer.packValue(new Variable().setBinaryValue(data.getBytes()));
        packer.close();
        return packer.toByteArray();
    }

}
