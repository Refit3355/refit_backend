package com.refit.app.domain.chat.mapper;

import com.refit.app.domain.chat.dto.response.ChatMessageResponse;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMapper {

    void insertFromRequest(Map<String, Object> param);

    ChatMessageResponse findByIdWithNickname(Long chatId);
}
