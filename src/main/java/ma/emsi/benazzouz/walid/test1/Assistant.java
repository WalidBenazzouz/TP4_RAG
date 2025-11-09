package ma.emsi.benazzouz.walid.test1;

import dev.langchain4j.service.SystemMessage;

public interface Assistant {

    @SystemMessage("Tu es un assistant RAG. Tu dois répondre uniquement à partir du PDF. " )
    String chat(String userMessage);
}
