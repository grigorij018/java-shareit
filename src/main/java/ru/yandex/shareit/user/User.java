package ru.yandex.shareit.user;

import lombok.Data;

@Data
public class User {
    private Long id;
    private String name;
    private String email;
}