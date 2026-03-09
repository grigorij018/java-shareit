package ru.practicum.shareit.request.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ItemRequestDto {
    private Long id;
    private String description;
    private LocalDateTime created;
    private List<ItemResponseDto> items;

    @Data
    public static class ItemResponseDto {
        private Long id;
        private String name;
        private String description;
        private Boolean available;
        private Long ownerId;
        private Long requestId;
    }
}