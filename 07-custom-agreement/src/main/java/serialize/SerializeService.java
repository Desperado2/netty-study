package serialize;

import java.io.IOException;

public interface SerializeService {

    String serialize(Object obj) throws IOException;

    Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException;
}
