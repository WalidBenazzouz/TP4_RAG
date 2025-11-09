package ma.emsi.benazzouz.walid.test2;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.embedding.Embedding;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;

import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

public class Test2 {

    public static void main(String[] args) {

        System.out.println("\n--- TEST 2 : RAG + Logging ---\n");

        // Charger PDF
        DocumentParser parser = new ApacheTikaDocumentParser();
        Path pdf = Paths.get("src/main/resources/support_rag.pdf");
        Document doc = FileSystemDocumentLoader.loadDocument(pdf, parser);

        // Segmenter
        var splitter = DocumentSplitters.recursive(300, 30);
        List<TextSegment> parts = splitter.split(doc);

        // Embeddings
        EmbeddingModel embedModel = new AllMiniLmL6V2EmbeddingModel();
        List<Embedding> vectors = embedModel.embedAll(parts).content();

        EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();
        store.addAll(vectors, parts);

        // Clé API
        String key = System.getenv("GEMINI_KEY");
        if (key == null) throw new IllegalStateException("⚠️ Variable d’environnement GEMINI_KEY manquante");

        ChatModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(key)
                .modelName("gemini-2.5-flash")
                .temperature(0.3)
                .logRequestsAndResponses(true)   // ✅ LOGGING pour Test2
                .build();

        var retriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(store)
                .embeddingModel(embedModel)
                .maxResults(2)
                .minScore(0.5)
                .build();

        var memory = MessageWindowChatMemory.withMaxMessages(10);

        Assistant bot = AiServices.builder(Assistant.class)
                .chatModel(model)
                .contentRetriever(retriever)
                .chatMemory(memory)
                .build();

        // Interaction console
        try (Scanner sc = new Scanner(System.in)) {
            System.out.println("Pose une question (exit pour quitter) :");
            while (true) {
                System.out.print("> ");
                String q = sc.nextLine();
                if (q.equalsIgnoreCase("exit")) break;
                System.out.println("\n🤖 " + bot.ask(q) + "\n");
            }
        }
    }
}
