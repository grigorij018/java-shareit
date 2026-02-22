package ru.yandex.shareit.item;

import ru.yandex.shareit.item.dto.ItemDto;
import java.util.List;

public interface ItemService {
    ItemDto createItem(Long userId, ItemDto itemDto);
    ItemDto updateItem(Long userId, Long itemId, ItemDto itemDto);
    ItemDto getItemById(Long id);
    List<ItemDto> getUserItems(Long userId);
    List<ItemDto> searchItems(String text);
}