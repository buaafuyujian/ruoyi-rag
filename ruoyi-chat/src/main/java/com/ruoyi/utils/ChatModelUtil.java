package com.ruoyi.utils;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.ai.zhipuai.ZhiPuAiChatOptions;
import org.springframework.ai.zhipuai.api.ZhiPuAiApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;

import java.util.List;

/**
 * 工具类，用于获取聊天模型
 */
@Slf4j
public class ChatModelUtil {
    /**
     * 获取OpenAI聊天模型
     * @param baseUrl
     * @param apiKey
     * @param model
     * @param restClientBuilderProvider 用于解决http2 不兼容问题
     * @param webClientBuilderProvider 用于解决http2 不兼容问题
     * @return
     */
    public static OpenAiChatModel getOpenAiChatModel(String baseUrl, String apiKey, String model, ObjectProvider<RestClient.Builder> restClientBuilderProvider,
                                                     ObjectProvider<WebClient.Builder> webClientBuilderProvider, ToolCallback... toolCallbacks) {

        // 创建使用 HTTP/1.1 的 WebClient（流式请求使用）
        HttpClient nettyHttpClient = HttpClient.create()
                                               .protocol(HttpProtocol.HTTP11);  // 强制使用 HTTP/1.1
        ReactorClientHttpConnector connector = new ReactorClientHttpConnector(nettyHttpClient);
        WebClient.Builder webClientBuilder = webClientBuilderProvider.getIfAvailable().clientConnector(connector);

        var openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .restClientBuilder(restClientBuilderProvider.getIfAvailable(RestClient::builder))
                .webClientBuilder(webClientBuilder)
                .build();
        var openAiChatOptions = OpenAiChatOptions.builder()
                .model(model)
                .temperature(0.4)
               // .maxTokens(200)
                .toolCallbacks(toolCallbacks)
                .build();

       return  OpenAiChatModel.builder().openAiApi(openAiApi).defaultOptions(openAiChatOptions).build();
        //return  new OpenAiChatModel(openAiApi, openAiChatOptions);
    }


    /**
     * 获取Ollama聊天模型
     * @param baseUrl
     * @param model
     * @return
     */
    public static OllamaChatModel getOllamaChatModel(String baseUrl, String model,ToolCallback... toolCallbacks) {
        //var ollamaApi = new OllamaApi(baseUrl);
        var ollamaApi = OllamaApi.builder().baseUrl(baseUrl).build();
        return  OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(
                        OllamaOptions.builder()
                                .model(model)
                                .temperature(0.4)
                                .build())
                .build();
    }


    /**
     * 获取智普AI聊天模型
     * @param baseUrl
     * @param apiKey
     * @param model
     * @return
     */
    public static ZhiPuAiChatModel getZhiPuAiChatModel(String baseUrl, String apiKey, String model, ToolCallback... toolCallbacks) {
        var zhiPuAiApi =  new ZhiPuAiApi(baseUrl,apiKey);
        var openAiChatOptions = ZhiPuAiChatOptions.builder()
                .model(model)
                .temperature(0.4)
                //.maxTokens(200)
               .toolCallbacks(toolCallbacks)
                .build();

        return  new ZhiPuAiChatModel(zhiPuAiApi, openAiChatOptions);
    }

    /**
     * 获取DashScope聊天模型
     * @param baseUrl
     * @param apiKey
     * @param model
     * @return
     */
    public static DashScopeChatModel getDashScopeChatModel(String baseUrl, String apiKey, String model, List<ToolCallback> toolCallbacks) {
        var dashScopeApi = DashScopeApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
        var openAiChatOptions = DashScopeChatOptions.builder()
                .withModel(model)
                .withTemperature(0.4)
                //.maxTokens(200)
                .withToolCallbacks(toolCallbacks)
                .build();

        return DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(openAiChatOptions)
                .build();
    }


}
