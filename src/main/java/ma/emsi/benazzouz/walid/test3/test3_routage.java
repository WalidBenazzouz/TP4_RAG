package ma.emsi.benazzouz.walid.test3;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.router.LanguageModelQueryRouter;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import ma.emsi.benazzouz.walid.test2.Assistant;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class test3_routage {

    // Activation du logging avancé
    private static void configureLogger() {
        Logger lc4jLogger = Logger.getLogger("dev.langchain4j");
        lc4jLogger.setLevel(Level.FINE);

        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.FINE);

        lc4jLogger.addHandler(handler);
    }

    public static void main(String[] args) {

        configureLogger();
        System.out.println("\n=== TEST 3 : Routage automatique des sources ===\n");

        // 1) Parser PDF + modèle d'embeddings
        DocumentParser parser = new ApacheTikaDocumentParser();
        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

        // 2) Chargement et découpage des documents
        List<TextSegment> segmentsIA = loadAndSplit("src/main/resources/support_rag.pdf", parser);
        List<TextSegment> segmentsVoiture = loadAndSplit("src/main/resources/voiture.pdf", parser); //

        // 3) Deux magasins indépendants
        EmbeddingStore<TextSegment> storeIA = new InMemoryEmbeddingStore<>();
        EmbeddingStore<TextSegment> storeVoiture = new InMemoryEmbeddingStore<>();

        storeIA.addAll(embeddingModel.embedAll(segmentsIA).content(), segmentsIA);
        storeVoiture.addAll(embeddingModel.embedAll(segmentsVoiture).content(), segmentsVoiture);

        // 4) Deux ContentRetrievers indépendants
        ContentRetriever retrieverIA = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(storeIA)
                .embeddingModel(embeddingModel)
                .maxResults(3)
                .minScore(0.5)
                .build();

        ContentRetriever retrieverVoiture = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(storeVoiture)
                .embeddingModel(embeddingModel)
                .maxResults(3)
                .minScore(0.5)
                .build();

        // 5) Modèle Gemini
        ChatModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(System.getenv("GEMINI_KEY"))
                .modelName("gemini-2.5-flash")
                .temperature(0.3)
                .logRequestsAndResponses(true)
                .build();

        // 6) Carte des sources avec descriptions → le LM lit ça pour router
        Map<ContentRetriever, String> sources = new HashMap<>();
        sources.put(retrieverIA, "Documents pédagogiques expliquant le RAG, l'IA et les LLM.");
        sources.put(retrieverVoiture, "Articles techniques sur les voitures, moteurs, performances et mécanique automobile.");

        // 7) Query Router
        var queryRouter = new LanguageModelQueryRouter(model, sources);

        // 8) RetrievalAugmentor basé sur le routeur
        var augmentor = DefaultRetrievalAugmentor.builder()
                .queryRouter(queryRouter)
                .build();

        // 9) Assistant final
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .retrievalAugmentor(augmentor)
                .build();

        // 10) Interaction utilisateur
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.print("\n👤 Vous : ");
            String question = sc.nextLine();
            if (question.equalsIgnoreCase("exit")) break;

            System.out.println("\n🤖 Gemini : " + assistant.ask(question));
        }
    }

    // Méthode utilitaire pour charger & splitter un PDF
    private static List<TextSegment> loadAndSplit(String chemin, DocumentParser parser) {
        Path path = Paths.get(chemin);
        Document doc = FileSystemDocumentLoader.loadDocument(path, parser);
        return DocumentSplitters.recursive(300, 30).split(doc);
    }
}
