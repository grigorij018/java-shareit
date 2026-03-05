package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.exception.ItemNotFoundException;
import ru.practicum.shareit.exception.UserNotFoundException;
import ru.practicum.shareit.item.dto.*;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final BookingRepository bookingRepository;

    @Override
    public ItemDto createItem(Long userId, ItemDto itemDto) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с id " + userId + " не найден"));

        Item item = ItemMapper.toItem(itemDto, owner);
        Item savedItem = itemRepository.save(item);
        return ItemMapper.toItemDto(savedItem);
    }

    @Override
    public ItemDto updateItem(Long userId, Long itemId, ItemDto itemDto) {
        // Проверяем существование пользователя
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с id " + userId + " не найден"));

        // Находим вещь
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ItemNotFoundException("Вещь с id " + itemId + " не найдена"));

        // Проверяем, что пользователь является владельцем
        if (!item.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException("Редактировать вещь может только её владелец");
        }

        // Обновляем только те поля, которые пришли в запросе
        if (itemDto.getName() != null) {
            item.setName(itemDto.getName());
        }
        if (itemDto.getDescription() != null) {
            item.setDescription(itemDto.getDescription());
        }
        if (itemDto.getAvailable() != null) {
            item.setAvailable(itemDto.getAvailable());
        }

        // Сохраняем изменения
        Item updatedItem = itemRepository.save(item);
        return ItemMapper.toItemDto(updatedItem);
    }

    @Override
    @Transactional(readOnly = true)
    public ItemDto getItemById(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Вещь с id " + id + " не найдена"));

        ItemDto itemDto = ItemMapper.toItemDto(item);

        // Добавляем комментарии
        List<CommentDto> comments = commentRepository.findAllByItemId(id).stream()
                .map(CommentMapper::toDto)
                .collect(Collectors.toList());
        itemDto.setComments(comments);

        return itemDto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemDto> getUserItems(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с id " + userId + " не найден"));

        LocalDateTime now = LocalDateTime.now();

        return itemRepository.findAllByOwner_IdOrderByIdAsc(userId).stream()
                .map(item -> {
                    ItemDto itemDto = ItemMapper.toItemDto(item);

                    // Добавляем комментарии
                    List<CommentDto> comments = commentRepository.findAllByItemId(item.getId()).stream()
                            .map(CommentMapper::toDto)
                            .collect(Collectors.toList());
                    itemDto.setComments(comments);

                    // Добавляем lastBooking и nextBooking для владельца
                    if (item.getOwner().getId().equals(userId)) {
                        // Находим последнее прошедшее бронирование
                        List<Booking> pastBookings = bookingRepository
                                .findAllByItemIdAndEndBeforeOrderByEndDesc(item.getId(), now);
                        if (!pastBookings.isEmpty()) {
                            Booking lastBooking = pastBookings.get(0);
                            ItemDto.BookingInfoDto lastBookingDto = new ItemDto.BookingInfoDto();
                            lastBookingDto.setId(lastBooking.getId());
                            lastBookingDto.setBookerId(lastBooking.getBooker().getId());
                            itemDto.setLastBooking(lastBookingDto);
                        }

                        // Находим следующее будущее бронирование
                        List<Booking> futureBookings = bookingRepository
                                .findAllByItemIdAndStartAfterOrderByStartAsc(item.getId(), now);
                        if (!futureBookings.isEmpty()) {
                            Booking nextBooking = futureBookings.get(0);
                            ItemDto.BookingInfoDto nextBookingDto = new ItemDto.BookingInfoDto();
                            nextBookingDto.setId(nextBooking.getId());
                            nextBookingDto.setBookerId(nextBooking.getBooker().getId());
                            itemDto.setNextBooking(nextBookingDto);
                        }
                    }

                    return itemDto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemDto> searchItems(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return itemRepository.searchAvailableByText(text).stream()
                .map(ItemMapper::toItemDto)
                .collect(Collectors.toList());
    }

    @Override
    public CommentDto addComment(Long userId, Long itemId, CommentCreateDto commentDto) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с id " + userId + " не найден"));

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ItemNotFoundException("Вещь с id " + itemId + " не найдена"));

        // Проверяем, что пользователь действительно брал вещь в аренду и аренда завершена
        List<Booking> completedBookings = bookingRepository
                .findAllByItemIdAndBookerIdAndEndBeforeAndStatus(
                        itemId, userId, LocalDateTime.now(), BookingStatus.APPROVED);

        if (completedBookings.isEmpty()) {
            throw new IllegalArgumentException("Вы можете оставить комментарий только после завершенной аренды");
        }

        Comment comment = CommentMapper.toEntity(commentDto, item, author);
        Comment savedComment = commentRepository.save(comment);
        return CommentMapper.toDto(savedComment);
    }
}