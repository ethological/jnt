package war.metaphor.resources;

import java.util.Map;

public interface IResource {

    String getName();

    String handle(String contents, Map<String, String> mapping);
}
