package ma.emsi.benazzouz.walid.test4;

import dev.langchain4j.data.document.*;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;

import java.nio.file.*;
import java.util.*;
import java.util.Scanner;

public class TestPasDeRag {

    public static void main(String[] args) {

        System.out.println("=== Test 4 : RAG seulement si pertinent ===");

        // 1) Load PDF
        Document doc = FileSystemDocumentLoader.loadDocument(
                Paths.get("src/main/resources/voiture.pdf"),
                new ApacheTikaDocumentParser()
        );

        List<TextSegment> segments = DocumentSplitters.recursive(300, 30).split(doc);

        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

        EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();
        store.addAll(embeddings, segments);

        // 2) Model
        String key = System.getenv("GEMINI_KEY");
        ChatModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(key)
                .modelName("gemini-2.5-flash")
                .temperature(0.3)
                .logRequestsAndResponses(true)
                .build();

        // 3) Retriever
        var retriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(store)
                .embeddingModel(embeddingModel)
                .maxResults(2)
                .minScore(0.5)
                .build();

        // 4) Router qui décide si RAG ou non
        QueryRouter router = query -> {
            String q = """
                    La question suivante concerne-t-elle le RAG, embeddings, fine-tuning ou IA ?
                    Réponds seulement par oui ou non :

                    Question : %s
                    """.formatted(query.text());

            String answer = model.chat(q).toLowerCase();

            if (answer.contains("oui")) {
                return List.of(retriever);
            }
            return List.of(); // Pas de RAG
        };

        RetrievalAugmentor augmentor = DefaultRetrievalAugmentor.builder()
                .queryRouter(router)
                .build();

        var assistant = AiServices.builder(AssistantLimite.class)
                .chatModel(model)
                .retrievalAugmentor(augmentor)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();

        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.print("\n👤 Vous : ");
            String q = sc.nextLine();
            if (q.equalsIgnoreCase("exit")) break;
            System.out.println("🤖 Gemini : " + assistant.chat(q));
        }
    }
}
