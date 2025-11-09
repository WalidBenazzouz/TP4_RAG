package ma.emsi.benazzouz.walid.test5;

import dev.langchain4j.data.document.*;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.content.retriever.WebSearchContentRetriever;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
import ma.emsi.benazzouz.walid.test4.AssistantLimite;
import java.nio.file.*;
import java.util.*;
import java.util.logging.*;

public class Test5_RagAvecWeb {

    private static void configureLogger() {
        Logger logger = Logger.getLogger("dev.langchain4j");
        logger.setLevel(Level.FINE);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.FINE);
        logger.addHandler(handler);
    }

    public static void main(String[] args) {

        configureLogger();
        System.out.println("=== Test 5 : RAG + Recherche Web Tavily ===");

        // 1) Parse & load PDF cours IA / RAG
        DocumentParser parser = new ApacheTikaDocumentParser();
        Path path = Paths.get("src/main/resources/voiture.pdf"); // Change ici si tu veux "voiture.pdf"
        Document document = FileSystemDocumentLoader.loadDocument(path, parser);

        var splitter = DocumentSplitters.recursive(300, 30);
        List<TextSegment> segments = splitter.split(document);

        // 2) Embeddings
        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

        // 3) Stockage des embeddings
        EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();
        store.addAll(embeddings, segments);

        System.out.println("✅ PDF chargé et indexé avec " + segments.size() + " segments.");

        // 4) Modèle Gemini
        String GEMINI_KEY = System.getenv("GEMINI_KEY");
        if (GEMINI_KEY == null) throw new IllegalStateException("❌ Variable GEMINI_KEY non trouvée.");

        ChatModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(GEMINI_KEY)
                .modelName("gemini-2.5-flash")
                .temperature(0.3)
                .logRequestsAndResponses(true)
                .build();

        // 5) Retriever local (PDF)
        ContentRetriever retrieverLocal = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(store)
                .embeddingModel(embeddingModel)
                .maxResults(3)
                .minScore(0.55)
                .build();

        // 6) Retriever Web (Tavily)
        String TAVILY_KEY = System.getenv("TAVILY_API_KEY");
        if (TAVILY_KEY == null) throw new IllegalStateException("❌ TAVILY_API_KEY manquent.");

        var tavilyEngine = TavilyWebSearchEngine.builder()
                .apiKey(TAVILY_KEY)
                .build();

        ContentRetriever retrieverWeb = WebSearchContentRetriever.builder()
                .webSearchEngine(tavilyEngine)
                .maxResults(4)
                .build();

        // 7) Routage = combine PDF + Web
        QueryRouter router = new DefaultQueryRouter(List.of(retrieverLocal, retrieverWeb));

        // 8) Augmentor
        RetrievalAugmentor augmentor = DefaultRetrievalAugmentor.builder()
                .queryRouter(router)
                .build();

        // 9) Assistant IA
        AssistantLimite assistant = AiServices.builder(AssistantLimite.class)
                .chatModel(model)
                .retrievalAugmentor(augmentor)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();

        // 10) Console
        try (Scanner sc = new Scanner(System.in)) {
            while (true) {
                System.out.print("\n👤 Vous : ");
                String question = sc.nextLine();
                if (question.equalsIgnoreCase("exit")) break;
                System.out.println("🤖 Gemini : " + assistant.chat(question));
            }
        }
    }
}
