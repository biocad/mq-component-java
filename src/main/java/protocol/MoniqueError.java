package protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Monique error object
 *
 * @author Pavel Didkovskii
 * */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class MoniqueError {

    private int code;

    private String message;

}
