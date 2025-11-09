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
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Test2_0 {

    public static void main(String[] args) {

        System.out.println("\n--- TEST 2.0 : Similarité seule (Embedding Search) ---\n");

        // 1) Charger PDF
        DocumentParser parser = new ApacheTikaDocumentParser();
        Path pdf = Paths.get("src/main/resources/support_rag.pdf");
        Document doc = FileSystemDocumentLoader.loadDocument(pdf, parser);

        // 2) Découpage
        var splitter = DocumentSplitters.recursive(300, 30);
        List<TextSegment> segments = splitter.split(doc);
        System.out.println("Nombre de segments = " + segments.size());

        // 3) Embeddings
        EmbeddingModel embedModel = new AllMiniLmL6V2EmbeddingModel();
        List<Embedding> vectors = embedModel.embedAll(segments).content();

        // 4) Stockage
        EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();
        store.addAll(vectors, segments);

        // 5) Question test
        String question = "C'est quoi le RAG ?";
        Embedding embeddingQuestion = embedModel.embed(question).content();

        // 6) Recherche vectorielle + Score
        EmbeddingSearchRequest req = EmbeddingSearchRequest.builder()
                .queryEmbedding(embeddingQuestion)
                .maxResults(5)
                .minScore(0.45)
                .build();

        EmbeddingSearchResult<TextSegment> result = store.search(req);

        System.out.println("🔍 Segments les plus pertinents :\n");
        for (EmbeddingMatch<TextSegment> match : result.matches()) {
            System.out.println("Score = " + match.score());
            System.out.println("→ " + match.embedded().text());
            System.out.println("-------------------------------------------------\n");
        }

        System.out.println("✅ FIN TEST 2.0");
    }
}
