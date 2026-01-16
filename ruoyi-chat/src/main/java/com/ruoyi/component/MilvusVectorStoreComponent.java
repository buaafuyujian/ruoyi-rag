package com.ruoyi.component;

import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.grpc.DataType;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.index.CreateIndexParam;
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
        if(!exists){
            // 不存在则创建集合
            createCollection(collectionName,openAiEmbeddingModel.dimensions());
            createIndex(collectionName);
        }

        MilvusVectorStore vectorStore = MilvusVectorStore.builder(milvusClient, openAiEmbeddingModel)
                                                         .collectionName(collectionName)
                                                         .indexType(IndexType.IVF_FLAT) // 可选：指定索引类型
                                                         .metricType(MetricType.COSINE) // 对应 Qdrant 的 Cosine
                                                         .build();
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


    /**
     * 参考自 org/springframework/ai/vectorstore/milvus/MilvusVectorStore.java
     */
    void createCollection(String collectionName,Integer embeddingDimensions) {
        FieldType docIdFieldType = FieldType.newBuilder()
                                            .withName(MilvusVectorStore.DOC_ID_FIELD_NAME)
                                            .withDataType(DataType.VarChar)
                                            .withMaxLength(36)
                                            .withPrimaryKey(true)
                                            .withAutoID(false)
                                            .build();
        FieldType contentFieldType = FieldType.newBuilder()
                                              .withName(MilvusVectorStore.CONTENT_FIELD_NAME)
                                              .withDataType(DataType.VarChar)
                                              .withMaxLength(65535)
                                              .build();
        FieldType metadataFieldType = FieldType.newBuilder()
                                               .withName(MilvusVectorStore.METADATA_FIELD_NAME)
                                               .withDataType(DataType.JSON)
                                               .build();
        FieldType embeddingFieldType = FieldType.newBuilder()
                                                .withName(MilvusVectorStore.EMBEDDING_FIELD_NAME)
                                                .withDataType(DataType.FloatVector)
                                                .withDimension(embeddingDimensions)
                                                .build();

        CreateCollectionParam createCollectionReq = CreateCollectionParam.newBuilder()
                                                                         .withDatabaseName(databaseName)
                                                                         .withCollectionName(collectionName)
                                                                         .withDescription("Spring AI Vector Store")
                                                                         .withConsistencyLevel(ConsistencyLevelEnum.STRONG)
                                                                         .withShardsNum(2)
                                                                         .addFieldType(docIdFieldType)
                                                                         .addFieldType(contentFieldType)
                                                                         .addFieldType(metadataFieldType)
                                                                         .addFieldType(embeddingFieldType)
                                                                         .build();

        R<RpcStatus> collectionStatus = this.milvusClient.createCollection(createCollectionReq);
        if (collectionStatus.getException() != null) {
            throw new RuntimeException("Failed to create collection", collectionStatus.getException());
        }

    }

    void createIndex(String collectionName) {
        R<RpcStatus> indexStatus = this.milvusClient.createIndex(CreateIndexParam.newBuilder()
                                                                                 .withDatabaseName(databaseName)
                                                                                 .withCollectionName(collectionName)
                                                                                 .withFieldName(MilvusVectorStore.EMBEDDING_FIELD_NAME)
                                                                                 .withIndexType(IndexType.IVF_FLAT)
                                                                                 .withMetricType(MetricType.COSINE)
                                                                                 .withExtraParam("{\"nlist\":1024}")
                                                                                 .withSyncMode(Boolean.FALSE)
                                                                                 .build());

        R<RpcStatus> loadCollectionStatus = this.milvusClient.loadCollection(LoadCollectionParam.newBuilder()
                                                                                               .withDatabaseName(this.databaseName)
                                                                                               .withCollectionName(collectionName)
                                                                                               .build());
    }
}

