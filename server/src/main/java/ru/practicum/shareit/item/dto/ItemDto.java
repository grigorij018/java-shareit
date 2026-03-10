package ru.practicum.shareit.item.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class ItemDto {
    private Long id;

    @NotBlank(message = "Название не может быть пустым")
    private String name;

    @NotBlank(message = "Описание не может быть пустым")
    private String description;

    @NotNull(message = "Статус доступности должен быть указан")
    private Boolean available;

    private Long requestId; // Новое поле

    private List<CommentDto> comments;
    private BookingInfoDto lastBooking;
    private BookingInfoDto nextBooking;

    @Data
    public static class BookingInfoDto {
        private Long id;
        private Long bookerId;
    }
}