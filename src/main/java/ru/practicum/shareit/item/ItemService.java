package ru.practicum.shareit.item;

import ru.practicum.shareit.item.dto.*;
import java.util.List;

public interface ItemService {
    ItemDto createItem(Long userId, ItemDto itemDto);
    ItemDto updateItem(Long userId, Long itemId, ItemDto itemDto);
    ItemDto getItemById(Long id);
    List<ItemDto> getUserItems(Long userId);
    List<ItemDto> searchItems(String text);
    CommentDto addComment(Long userId, Long itemId, CommentCreateDto commentDto);
}