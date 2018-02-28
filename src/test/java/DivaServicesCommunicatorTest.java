import diva.DivaServicesAdmin;
import exceptions.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Marcel Würsch
 *         marcel.wuersch@unifr.ch
 *         http://diuf.unifr.ch/main/diva/home/people/marcel-w%C3%BCrsch
 *         Created on: 5/18/2017.
 */
public class DivaServicesCommunicatorTest {

    @Test
    public void testBinarization() throws MethodNotAvailableException, IOException, ForgotKeyValueObjectException, IncompatibleValueException, UserValueRequiredException, FileTypeConfusionException, UserParametersOverloadException {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("inputImage", "ghostwhitesteepmyna/csg562-009.jpg");

        DivaServicesAdmin.runMethod("http://divaservices.unifr.ch/api/v2/binarization/krakenbinarization/1",parameters);

    }

}
