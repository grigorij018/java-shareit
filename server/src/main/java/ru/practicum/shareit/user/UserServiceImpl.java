package ru.practicum.shareit.user;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.dto.UserDto;
import ru.practicum.shareit.dto.UserMapper;
import ru.practicum.shareit.exception.UserNotFoundException;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private final UserRepository userRepository;

    @Override
    public UserDto createUser(UserDto userDto) {
        validateUserForCreate(userDto);
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new ConflictException("Пользователь с email " + userDto.getEmail() + " уже существует");
        }

        try {
            User user = UserMapper.toUser(userDto);
            User savedUser = userRepository.save(user);
            return UserMapper.toUserDto(savedUser);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Пользователь с email " + userDto.getEmail() + " уже существует");
        }
    }

    @Override
    public UserDto updateUser(Long userId, UserDto userDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        validateUserForUpdate(userDto);

        if (userDto.getName() != null) {
            user.setName(userDto.getName());
        }

        if (userDto.getEmail() != null && !userDto.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmailAndIdNot(userDto.getEmail(), userId)) {
                throw new ConflictException("Пользователь с email " + userDto.getEmail() + " уже существует");
            }
            user.setEmail(userDto.getEmail());
        }

        try {
            User updatedUser = userRepository.save(user);
            return UserMapper.toUserDto(updatedUser);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Пользователь с email " + userDto.getEmail() + " уже существует");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));
        return UserMapper.toUserDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserMapper::toUserDto)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("Пользователь не найден");
        }
        userRepository.deleteById(id);
    }

    private void validateUserForCreate(UserDto userDto) {
        if (userDto == null || userDto.getName() == null || userDto.getName().isBlank()) {
            throw new IllegalArgumentException("Имя не может быть пустым");
        }
        if (userDto.getEmail() == null || userDto.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email не может быть пустым");
        }
        if (!EMAIL_PATTERN.matcher(userDto.getEmail()).matches()) {
            throw new IllegalArgumentException("Некорректный формат email");
        }
    }

    private void validateUserForUpdate(UserDto userDto) {
        if (userDto == null) {
            return;
        }
        if (userDto.getName() != null && userDto.getName().isBlank()) {
            throw new IllegalArgumentException("Имя не может быть пустым");
        }
        if (userDto.getEmail() != null) {
            if (userDto.getEmail().isBlank()) {
                throw new IllegalArgumentException("Email не может быть пустым");
            }
            if (!EMAIL_PATTERN.matcher(userDto.getEmail()).matches()) {
                throw new IllegalArgumentException("Некорректный формат email");
            }
        }
    }
}
