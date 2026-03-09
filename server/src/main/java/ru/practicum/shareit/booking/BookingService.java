package ru.practicum.shareit.booking;

import ru.practicum.shareit.dto.BookingCreateDto;
import ru.practicum.shareit.dto.BookingDto;
import ru.practicum.shareit.dto.BookingState;
import java.util.List;

public interface BookingService {
    BookingDto create(Long userId, BookingCreateDto dto);

    BookingDto approve(Long ownerId, Long bookingId, boolean approved);

    BookingDto getById(Long userId, Long bookingId);

    List<BookingDto> getUserBookings(Long userId, BookingState state, Integer from, Integer size);

    List<BookingDto> getOwnerBookings(Long ownerId, BookingState state, Integer from, Integer size);
}