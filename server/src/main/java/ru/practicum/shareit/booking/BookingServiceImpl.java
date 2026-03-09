package ru.practicum.shareit.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.dto.BookingCreateDto;
import ru.practicum.shareit.dto.BookingDto;
import ru.practicum.shareit.dto.BookingState;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.exception.BookingNotFoundException;
import ru.practicum.shareit.exception.ItemNotFoundException;
import ru.practicum.shareit.exception.UserNotFoundException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    @Override
    public BookingDto create(Long userId, BookingCreateDto dto) {
        // Проверяем существование пользователя
        User booker = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с id " + userId + " не найден"));

        // Проверяем существование вещи
        Item item = itemRepository.findById(dto.getItemId())
                .orElseThrow(() -> new ItemNotFoundException("Вещь с id " + dto.getItemId() + " не найдена"));

        // Проверяем, что пользователь не является владельцем вещи
        if (item.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException("Нельзя забронировать свою собственную вещь");
        }

        // Проверяем доступность вещи
        if (!Boolean.TRUE.equals(item.getAvailable())) {
            throw new IllegalArgumentException("Вещь недоступна для бронирования");
        }

        // Проверяем корректность дат
        if (dto.getStart().isAfter(dto.getEnd()) || dto.getStart().isEqual(dto.getEnd())) {
            throw new IllegalArgumentException("Дата окончания бронирования должна быть позже даты начала");
        }

        if (dto.getStart().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Дата начала бронирования не может быть в прошлом");
        }

        // Проверяем пересечение с существующими бронированиями
        if (bookingRepository.hasApprovedOverlap(item.getId(), dto.getStart(), dto.getEnd())) {
            throw new IllegalArgumentException("Вещь уже забронирована на указанные даты");
        }

        // Создаем бронирование
        Booking booking = BookingMapper.toEntity(dto, item, booker);
        Booking savedBooking = bookingRepository.save(booking);

        return BookingMapper.toDto(savedBooking);
    }

    @Override
    public BookingDto approve(Long ownerId, Long bookingId, boolean approved) {
        // Проверяем существование бронирования
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Бронирование с id " + bookingId + " не найдено"));

        // Проверяем, что пользователь является владельцем вещи
        if (!booking.getItem().getOwner().getId().equals(ownerId)) {
            throw new IllegalArgumentException("Подтверждать бронирование может только владелец вещи");
        }

        // Проверяем статус бронирования
        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new IllegalArgumentException("Можно подтвердить или отклонить только бронирование со статусом WAITING");
        }

        // Обновляем статус
        booking.setStatus(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED);
        Booking updatedBooking = bookingRepository.save(booking);

        return BookingMapper.toDto(updatedBooking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingDto getById(Long userId, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Бронирование с id " + bookingId + " не найдено"));

        // Проверяем права доступа (автор бронирования или владелец вещи)
        boolean isBooker = booking.getBooker().getId().equals(userId);
        boolean isOwner = booking.getItem().getOwner().getId().equals(userId);

        if (!isBooker && !isOwner) {
            throw new IllegalArgumentException("Просмотр бронирования доступен только автору или владельцу вещи");
        }

        return BookingMapper.toDto(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingDto> getUserBookings(Long userId, BookingState state, Integer from, Integer size) {
        // Проверяем существование пользователя
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с id " + userId + " не найден"));

        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings;

        switch (state) {
            case ALL:
                bookings = bookingRepository.findAllByBookerIdOrderByStartDesc(userId);
                break;
            case CURRENT:
                bookings = bookingRepository.findCurrentByBooker(userId, now);
                break;
            case PAST:
                bookings = bookingRepository.findAllByBookerIdAndEndBeforeAndStatusOrderByStartDesc(
                        userId, now, BookingStatus.APPROVED);
                break;
            case FUTURE:
                bookings = bookingRepository.findAllByBookerIdAndStartAfterAndStatusOrderByStartDesc(
                        userId, now, BookingStatus.APPROVED);
                break;
            case WAITING:
                bookings = bookingRepository.findAllByBookerIdAndStatusOrderByStartDesc(userId, BookingStatus.WAITING);
                break;
            case REJECTED:
                bookings = bookingRepository.findAllByBookerIdAndStatusOrderByStartDesc(userId, BookingStatus.REJECTED);
                break;
            default:
                throw new IllegalArgumentException("Неподдерживаемый статус: " + state);
        }

        return applyPagination(bookings.stream()
                .map(BookingMapper::toDto)
                .collect(Collectors.toList()), from, size);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingDto> getOwnerBookings(Long ownerId, BookingState state, Integer from, Integer size) {
        // Проверяем существование пользователя
        userRepository.findById(ownerId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с id " + ownerId + " не найден"));

        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings;

        switch (state) {
            case ALL:
                bookings = bookingRepository.findAllByItem_Owner_IdOrderByStartDesc(ownerId);
                break;
            case CURRENT:
                bookings = bookingRepository.findCurrentByOwner(ownerId, now);
                break;
            case PAST:
                bookings = bookingRepository.findAllByItem_Owner_IdAndEndBeforeAndStatusOrderByStartDesc(
                        ownerId, now, BookingStatus.APPROVED);
                break;
            case FUTURE:
                bookings = bookingRepository.findAllByItem_Owner_IdAndStartAfterAndStatusOrderByStartDesc(
                        ownerId, now, BookingStatus.APPROVED);
                break;
            case WAITING:
                bookings = bookingRepository.findAllByItem_Owner_IdAndStatusOrderByStartDesc(ownerId, BookingStatus.WAITING);
                break;
            case REJECTED:
                bookings = bookingRepository.findAllByItem_Owner_IdAndStatusOrderByStartDesc(ownerId, BookingStatus.REJECTED);
                break;
            default:
                throw new IllegalArgumentException("Неподдерживаемый статус: " + state);
        }

        return applyPagination(bookings.stream()
                .map(BookingMapper::toDto)
                .collect(Collectors.toList()), from, size);
    }

    private List<BookingDto> applyPagination(List<BookingDto> bookings, Integer from, Integer size) {
        if (from < 0 || size <= 0) {
            throw new IllegalArgumentException("Параметры пагинации указаны некорректно");
        }
        if (from >= bookings.size()) {
            return List.of();
        }
        int toIndex = Math.min(from + size, bookings.size());
        return bookings.subList(from, toIndex);
    }
}
