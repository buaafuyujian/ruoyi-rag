package com.ruoyi.component;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.collection.HasCollectionParam;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Milvus向量存储组件
 */
@Component
public class MilvusVectorStoreComponent {

    @Autowired
    private MilvusServiceClient milvusClient;

    @Autowired
    private OpenAiEmbeddingModel openAiEmbeddingModel;

    @Value("${spring.ai.vectorstore.milvus.database-name:default}")
    private String databaseName;

    @Value("${spring.ai.vectorstore.milvus.collection-name:rag}")
    private String collectionName;

    // 向量维度，与embedding模型bge-small-zh-v1.5匹配
    private static final int EMBEDDING_DIMENSION = 512;

    /**
     * 获取Milvus向量存储组件
     * @param collectionName 集合名称
     * @return MilvusVectorStore
     */
    public MilvusVectorStore getVectorStore(String collectionName) throws Exception {
        // 1. 检查集合是否存在
        R<Boolean> response = milvusClient.hasCollection(
                HasCollectionParam.newBuilder()
                                  .withCollectionName(collectionName)
                                  .build()
        );

        boolean exists = response.getData();

        // 2. 构造 VectorStore
        // 如果 exists 为 false，且 initializeSchema 设为 true，
        // Spring AI 会自动按照默认配置创建集合并建立索引
        MilvusVectorStore vectorStore = MilvusVectorStore.builder(milvusClient, openAiEmbeddingModel)
                                                      .collectionName(collectionName)
                                                      .databaseName("default")
                                                      .initializeSchema(!exists) // 如果不存在，则允许初始化
                                                      .indexType(IndexType.IVF_FLAT) // 可选：指定索引类型
                                                      .metricType(MetricType.COSINE) // 对应 Qdrant 的 Cosine
                                                      .build();
        try {
            // 这行代码会执行真正创建 Collection、Field 和 Index 的操作
            if(!exists){
                vectorStore.afterPropertiesSet();
            }
        } catch (Exception e) {
            throw new RuntimeException("初始化 Milvus 集合失败: " + collectionName, e);
        }
        return vectorStore;
    }

    /**
     * 覆盖自动配置中的MilvusVectorStore Bean
     */
    @Bean
    @Primary
    public MilvusVectorStore customMilvusVectorStore() {
        return MilvusVectorStore.builder(milvusClient, openAiEmbeddingModel)
                .databaseName(databaseName)
                .collectionName(collectionName)
                .embeddingDimension(EMBEDDING_DIMENSION)
                .initializeSchema(true)  // 确保自动创建schema
                .build();
    }
}

