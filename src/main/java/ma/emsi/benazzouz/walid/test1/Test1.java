package ma.emsi.benazzouz.walid.test1;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;

import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;

import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

public class RagNaif {

    public static void main(String[] args) {

        System.out.println("\n--- TEST 1 : RAG test1 (manuel) ---\n");

        // Charger PDF
        DocumentParser parser = new ApacheTikaDocumentParser();
        Path pdf = Paths.get("src/main/resources/support_rag.pdf");
        Document document = FileSystemDocumentLoader.loadDocument(pdf, parser);

        // Découper en fragments
// Découpage plus fin → meilleur rappel
        List<TextSegment> fragments = DocumentSplitters.recursive(150, 30).split(document);

        // Générer embeddings
        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        List<Embedding> vectors = embeddingModel.embedAll(fragments).content();

        // Stockage RAM
        EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();
        store.addAll(vectors, fragments);

        // Clé Gemini
        String key = System.getenv("GEMINI_KEY");
        if (key == null) throw new IllegalStateException("Définis GEMINI_KEY.");

        // Modèle IA
        ChatModel llm = GoogleAiGeminiChatModel.builder()
                .apiKey(key)
                .modelName("gemini-2.5-flash")
                .temperature(0.3)
                .build();

        // Récupérateur de contexte
        var retriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(store)
                .embeddingModel(embeddingModel)
                .maxResults(5)
                .minScore(0.1)
                .build();

        // Mémoire conversationnelle
        var memory = MessageWindowChatMemory.withMaxMessages(10);

        // Assistant RAG
        Assistant bot = AiServices.builder(Assistant.class)
                .chatModel(llm)
                .contentRetriever(retriever)
                .chatMemory(memory)
                .build();

        // Console interactive
        try (Scanner sc = new Scanner(System.in)) {
            System.out.println("Pose une question (exit pour quitter) :");
            while (true) {
                System.out.print("> ");
                String q = sc.nextLine().trim();

                if (q.equalsIgnoreCase("exit")) break;
                if (q.isEmpty()) continue; // ✅ ignore input vide

                System.out.println("\n" + bot.chat(q) + "\n");
            }

        }
    }
}
