package ma.emsi.benazzouz.walid.test2;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface Assistant {

    @SystemMessage("""
            Tu es un assistant pédagogique.
            Explique de façon claire, simple et structurée.
            Utilise des exemples courts.
            Réponds en français.
            """)
    @UserMessage("Question : {{message}}")
    String ask(String message);
}
