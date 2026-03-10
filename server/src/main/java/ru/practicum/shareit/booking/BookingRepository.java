package ru.practicum.shareit.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.shareit.booking.model.BookingStatus;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Для booker
    List<Booking> findAllByBookerIdOrderByStartDesc(Long bookerId);

    @Query("SELECT b FROM Booking b " +
            "WHERE b.booker.id = :bookerId " +
            "AND b.status = 'APPROVED' " +
            "AND :now BETWEEN b.start AND b.end " +
            "ORDER BY b.start DESC")
    List<Booking> findCurrentByBooker(@Param("bookerId") Long bookerId, @Param("now") LocalDateTime now);


    List<Booking> findAllByBookerIdAndEndBeforeOrderByStartDesc(Long bookerId, LocalDateTime now);

    List<Booking> findAllByBookerIdAndStartAfterOrderByStartDesc(Long bookerId, LocalDateTime now);

    List<Booking> findAllByBookerIdAndStatusOrderByStartDesc(Long bookerId, BookingStatus status);

    // Для owner - ИСПРАВЛЕНО: используем правильный синтаксис с подчеркиванием
    List<Booking> findAllByItem_Owner_IdOrderByStartDesc(Long ownerId);

    @Query("SELECT b FROM Booking b " +
            "WHERE b.item.owner.id = :ownerId " +
            "AND b.status = 'APPROVED' " +
            "AND :now BETWEEN b.start AND b.end " +
            "ORDER BY b.start DESC")
    List<Booking> findCurrentByOwner(@Param("ownerId") Long ownerId, @Param("now") LocalDateTime now);

    List<Booking> findAllByItem_Owner_IdAndEndBeforeOrderByStartDesc(Long ownerId, LocalDateTime now);

    List<Booking> findAllByItem_Owner_IdAndStartAfterOrderByStartDesc(Long ownerId, LocalDateTime now);

    List<Booking> findAllByItem_Owner_IdAndStatusOrderByStartDesc(Long ownerId, BookingStatus status);

    // Для поиска по itemId
    List<Booking> findAllByItemIdAndEndBeforeOrderByEndDesc(Long itemId, LocalDateTime end);

    List<Booking> findAllByItemIdAndStartAfterOrderByStartAsc(Long itemId, LocalDateTime start);

    List<Booking> findAllByItemIdAndBookerIdAndEndBeforeAndStatus(
            Long itemId, Long bookerId, LocalDateTime end, BookingStatus status);

    // Проверка для комментариев
    boolean existsByItemIdAndBookerIdAndEndBeforeAndStatus(
            Long itemId, Long bookerId, LocalDateTime endBefore, BookingStatus status);

    // Проверка пересечения бронирований
    @Query("SELECT COUNT(b) > 0 FROM Booking b " +
            "WHERE b.item.id = :itemId " +
            "AND b.status = 'APPROVED' " +
            "AND (:start < b.end AND :end > b.start)")
    boolean hasApprovedOverlap(@Param("itemId") Long itemId,
                               @Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end);

    @Query("SELECT b FROM Booking b " +
            "WHERE b.item.id IN :itemIds " +
            "AND b.end < :now " +
            "AND b.status = 'APPROVED' " +
            "AND b.end = (SELECT MAX(b2.end) FROM Booking b2 WHERE b2.item.id = b.item.id AND b2.end < :now)")
    List<Booking> findLastBookingsForItems(@Param("itemIds") List<Long> itemIds, @Param("now") LocalDateTime now);

    @Query("SELECT b FROM Booking b " +
            "WHERE b.item.id IN :itemIds " +
            "AND b.start > :now " +
            "AND b.status = 'APPROVED' " +
            "AND b.start = (SELECT MIN(b2.start) FROM Booking b2 WHERE b2.item.id = b.item.id AND b2.start > :now)")
    List<Booking> findNextBookingsForItems(@Param("itemIds") List<Long> itemIds, @Param("now") LocalDateTime now);

    List<Booking> findAllByBookerIdAndEndBeforeAndStatusOrderByStartDesc(Long bookerId, LocalDateTime now, BookingStatus status);

    List<Booking> findAllByBookerIdAndStartAfterAndStatusOrderByStartDesc(Long bookerId, LocalDateTime now, BookingStatus status);

    List<Booking> findAllByItem_Owner_IdAndEndBeforeAndStatusOrderByStartDesc(Long ownerId, LocalDateTime now, BookingStatus status);

    List<Booking> findAllByItem_Owner_IdAndStartAfterAndStatusOrderByStartDesc(Long ownerId, LocalDateTime now, BookingStatus status);

    boolean existsByItemIdAndBookerIdAndEndBefore(Long itemId, Long bookerId, LocalDateTime endBefore);
}