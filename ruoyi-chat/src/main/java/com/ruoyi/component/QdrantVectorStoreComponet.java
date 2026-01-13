package com.ruoyi.component;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.ai.vectorstore.qdrant.autoconfigure.QdrantVectorStoreProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * Qdrant向量存储组件
 */
@Component
public class QdrantVectorStoreComponet {

    @Autowired
    private QdrantClient qdrantClient;

    @Autowired
    private QdrantVectorStoreProperties properties;

    @Autowired
    private OpenAiEmbeddingModel openAiEmbeddingModel;

    /**
     * 获取Qdrant向量存储组件
     * @param collectionName 集合名称
     * @return
     * @throws Exception
     */
    public QdrantVectorStore getVectorStore (String collectionName ) throws Exception {
        if (!qdrantClient.collectionExistsAsync(collectionName).get()) {
            qdrantClient.createCollectionAsync(collectionName,
                    Collections.VectorParams.newBuilder()
                            .setDistance(Collections.Distance.Cosine)
                            .setSize(512)
                            .build()).get();
        }

        return QdrantVectorStore.builder(qdrantClient, openAiEmbeddingModel)
            .collectionName(collectionName)
            .initializeSchema(properties.isInitializeSchema())
            .build();
    }

    // 覆盖QdrantVectorStoreAutoConfiguration 中的自动注入
    @Bean
    public QdrantVectorStore customQdrantVectorStore() {
        return QdrantVectorStore.builder(qdrantClient, openAiEmbeddingModel)
                                .initializeSchema(properties.isInitializeSchema())
                                .build();
    }

}
