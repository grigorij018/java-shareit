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
import java.util.Map;
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
        // 1. Проверяем пользователя
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с id " + userId + " не найден"));

        LocalDateTime now = LocalDateTime.now();

        // 2. Загружаем товары владельца
        List<Item> items = itemRepository.findAllByOwner_IdOrderByIdAsc(userId);

        if (items.isEmpty()) {
            return List.of();
        }

        // 3. Собираем ID всех товаров
        List<Long> itemIds = items.stream()
                .map(Item::getId)
                .collect(Collectors.toList());

        // 4. Загружаем ВСЕ комментарии для этих товаров (ОДНИМ запросом)
        List<Comment> allComments = commentRepository.findAllByItemIdIn(itemIds);

        // Группируем комментарии по itemId, используя сам объект Comment
        Map<Long, List<CommentDto>> commentsByItemId = allComments.stream()
                .collect(Collectors.groupingBy(
                        comment -> comment.getItem().getId(),  // Получаем itemId из связи
                        Collectors.mapping(CommentMapper::toDto, Collectors.toList())
                ));

        // 5. Загружаем ВСЕ прошлые бронирования
        List<Booking> lastBookings = bookingRepository.findLastBookingsForItems(itemIds, now);
        Map<Long, Booking> lastBookingByItemId = lastBookings.stream()
                .collect(Collectors.toMap(
                        booking -> booking.getItem().getId(),  // Получаем itemId из связи
                        booking -> booking,
                        (existing, replacement) -> existing
                ));

        // 6. Загружаем ВСЕ будущие бронирования
        List<Booking> nextBookings = bookingRepository.findNextBookingsForItems(itemIds, now);
        Map<Long, Booking> nextBookingByItemId = nextBookings.stream()
                .collect(Collectors.toMap(
                        booking -> booking.getItem().getId(),
                        booking -> booking,
                        (existing, replacement) -> existing
                ));

        // 7. Формируем результат
        return items.stream()
                .map(item -> {
                    ItemDto itemDto = ItemMapper.toItemDto(item);

                    // Добавляем комментарии из заранее подготовленной мапы
                    itemDto.setComments(commentsByItemId.getOrDefault(item.getId(), List.of()));

                    // Добавляем бронирования для владельца
                    if (item.getOwner().getId().equals(userId)) {
                        Booking lastBooking = lastBookingByItemId.get(item.getId());
                        if (lastBooking != null) {
                            ItemDto.BookingInfoDto lastBookingDto = new ItemDto.BookingInfoDto();
                            lastBookingDto.setId(lastBooking.getId());
                            lastBookingDto.setBookerId(lastBooking.getBooker().getId());
                            itemDto.setLastBooking(lastBookingDto);
                        }

                        Booking nextBooking = nextBookingByItemId.get(item.getId());
                        if (nextBooking != null) {
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