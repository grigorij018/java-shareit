package ru.practicum.shareit.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookingDto {
    private Long id;
    private LocalDateTime start;
    private LocalDateTime end;
    private String status;
    private ItemDto item;
    private UserDto booker;

    @Data
    public static class ItemDto {
        private Long id;
        private String name;
    }

    @Data
    public static class UserDto {
        private Long id;
        private String name;
    }
}