package ru.practicum.shareit.request.dto;

import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.item.model.Item;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class ItemRequestMapper {

    public static ItemRequestDto toDto(ItemRequest request) {
        if (request == null) {
            return null;
        }

        ItemRequestDto dto = new ItemRequestDto();
        dto.setId(request.getId());
        dto.setDescription(request.getDescription());
        dto.setCreated(request.getCreated());
        dto.setItems(Collections.emptyList());
        return dto;
    }

    public static ItemRequestDto toDtoWithItems(ItemRequest request, List<ItemRequestDto.ItemResponseDto> items) {
        ItemRequestDto dto = toDto(request);
        if (items != null) {
            dto.setItems(items);
        }
        return dto;
    }

    public static ItemRequest toEntity(ItemRequestCreateDto dto, User requestor) {
        ItemRequest request = new ItemRequest();
        request.setDescription(dto.getDescription());
        request.setRequestor(requestor);
        request.setCreated(LocalDateTime.now());
        return request;
    }

    public static ItemRequestDto.ItemResponseDto toItemResponseDto(Item item) {
        if (item == null) {
            return null;
        }

        ItemRequestDto.ItemResponseDto dto = new ItemRequestDto.ItemResponseDto();
        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setDescription(item.getDescription());
        dto.setAvailable(item.getAvailable());
        dto.setOwnerId(item.getOwner().getId());
        dto.setRequestId(item.getRequest() != null ? item.getRequest().getId() : null);
        return dto;
    }
}
