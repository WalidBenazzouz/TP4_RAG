package ma.emsi.benazzouz.walid.test_rag_naif;

import dev.langchain4j.model.gemini.GoogleAiGeminiChatModel;

public class Test1SimpleLLM {

    public static void run() {

        // On crée le modèle Gemini en utilisant la clé définie dans ton système (GEMINI_KEY)
        GoogleAiGeminiChatModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(System.getenv("GEMINI_KEY"))
                .modelName("gemini-pro") // modèle texte simple
                .build();

        // Message à envoyer au modèle
        String userMessage = "Explique-moi simplement le concept de RAG en intelligence artificielle.";

        // Appel du modèle
        String response = model.generate(userMessage);

        // Affichage
        System.out.println("\n========== TEST 1 : Réponse du modèle ==========");
        System.out.println(response);
        System.out.println("================================================\n");
    }
}
