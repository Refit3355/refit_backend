package com.refit.app.domain.chat.mapper;

import com.refit.app.domain.chat.dto.ChatMessageDto;
import com.refit.app.domain.chat.dto.ChatRoomDto;
import com.refit.app.domain.chat.dto.response.ChatMessageResponse;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ChatMapper {

    void insertFromRequest(Map<String, Object> param);

    ChatMessageResponse findByIdWithNickname(@Param("chatId") Long chatId);

    List<ChatMessageResponse> findHistory(Map<String, Object> param);

    int existsOlder(@Param("categoryId") Long categoryId,
            @Param("beforeId") Long beforeId);

    List<ChatRoomDto> findRoomsByTab(@Param("tab") String tab);
}
