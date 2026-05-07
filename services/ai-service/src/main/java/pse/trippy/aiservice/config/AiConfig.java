package pse.trippy.aiservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class AiConfig {

    @Bean
    public ChatClient chatClient(OpenAiChatModel model) {
        return ChatClient.builder(model)
                .defaultSystem("""
                        You are Trippy AI, a helpful travel planning assistant.
                        Always respond with valid JSON when asked for structured data.
                        Never include markdown code fences or extra commentary in your JSON responses.
                        """)
                .build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Bean(destroyMethod = "shutdownNow")
    public ExecutorService aiBlockingExecutor() {
        AtomicInteger threadNumber = new AtomicInteger(1);
        return Executors.newFixedThreadPool(4, task -> {
            Thread thread = new Thread(task, "ai-blocking-" + threadNumber.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        });
    }
}
