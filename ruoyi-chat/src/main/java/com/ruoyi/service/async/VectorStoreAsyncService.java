package com.ruoyi.service.async;

import com.ruoyi.domain.ChatFile;
import com.ruoyi.service.IChatFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 异步处理向量存储
 */
@Service
@Slf4j
public class VectorStoreAsyncService {

    @Autowired
    IChatFileService chatFileService;

    /**
     * 异步执行根据fileId删除向量存储
     * @param vectorStore
     * @param fileId
     * @throws Exception
     */
    @Async
    public void removeByFileId(VectorStore vectorStore, String fileId) throws Exception {
        vectorStore.delete(
                new FilterExpressionBuilder().eq("fileId", fileId).build()
        );
        log.info("异步执行根据fileId删除向量存储成功");

    }

    /**
     * 异步执行新增向量存储
     * @param vectorStore
     * @param documentList
     * @throws Exception
     */
    @Async
    public void addVectorStore(String fileId, VectorStore vectorStore, List<Document> documentList) throws Exception {
        if (!CollectionUtils.isEmpty(documentList)) {
            vectorStore.add(documentList);
            log.info("异步执行新增向量存储成功");
        }

        ChatFile chatFile = new ChatFile();
        chatFile.setFileId(fileId);
        chatFile.setIsVector(1);
        chatFileService.updateChatFile(chatFile);
        log.info("异步修改文件是否向量化完成为完成状态");
    }
}
