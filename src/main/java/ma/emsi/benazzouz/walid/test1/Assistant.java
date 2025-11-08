package ma.emsi.benazzouz.walid;

import dev.langchain4j.service.UserMessage;

@FunctionalInterface
public interface Assistant {

    String chat(@UserMessage String userMessage);
}
