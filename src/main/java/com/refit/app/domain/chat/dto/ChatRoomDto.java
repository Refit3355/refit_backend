package com.refit.app.domain.chat.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ChatRoomDto {
    private Long categoryId;
    private String categoryName;   // 없으면 null 허용
    private Long lastChatId;       // 정렬/커서용
    private String lastMessage;    // 미리보기(없으면 null)
    private LocalDateTime lastAt;      // 마지막 메시지 시각(없으면 null)
}
