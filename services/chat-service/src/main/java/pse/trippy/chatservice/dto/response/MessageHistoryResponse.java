package pse.trippy.chatservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageHistoryResponse {

    private List<ChatMessageResponse> messages;
    private int page;
    private int size;
    private long totalMessages;
    private boolean hasMore;
}
