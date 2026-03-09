package ru.practicum.shareit.gateway.item.dto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import ru.practicum.shareit.item.dto.ItemDto;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class ItemDtoJsonTest {

    @Autowired
    private JacksonTester<ItemDto> json;

    @Test
    void testItemDtoSerialization() throws Exception {
        ItemDto itemDto = new ItemDto();
        itemDto.setId(1L);
        itemDto.setName("Test Item");
        itemDto.setDescription("Test Description");
        itemDto.setAvailable(true);
        itemDto.setRequestId(5L);

        assertThat(json.write(itemDto))
                .hasJsonPathNumberValue("$.id", 1)
                .hasJsonPathStringValue("$.name", "Test Item")
                .hasJsonPathStringValue("$.description", "Test Description")
                .hasJsonPathBooleanValue("$.available", true)
                .hasJsonPathNumberValue("$.requestId", 5);
    }
}