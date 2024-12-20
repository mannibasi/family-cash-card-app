package org.familycashcardapp;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
public class CashCardJsonTest {
    @Autowired
    private JacksonTester<CashCard> json;

    @Autowired
    private JacksonTester<CashCard[]> jsonList;

    private CashCard[] cashCards;

    @BeforeEach
    void setUp() {
        cashCards = Arrays.array(
                new CashCard(99L, 123.45, "Manni"),
                new CashCard(100L, 1.00, "Toni"),
                new CashCard(101L, 150.00, "Trish"));
    }

    @Test
    void cashCardSerializationTest() throws IOException {
        CashCard cashCard = new CashCard(99L, 123.45, "Manni");
        assertThat(json.write(cashCard)).isStrictlyEqualToJson("single.json");
        assertThat(json.write(cashCard)).hasJsonPathNumberValue("@.id");
        assertThat(json.write(cashCard)).hasJsonPathNumberValue("@.amount");
        assertThat(json.write(cashCard)).hasJsonPathStringValue("@.owner");
        assertThat(json.write(cashCard)).extractingJsonPathNumberValue("@.id").isEqualTo(99);
        assertThat(json.write(cashCard)).extractingJsonPathNumberValue("@.amount").isEqualTo(123.45);
        assertThat(json.write(cashCard)).extractingJsonPathStringValue("@.owner").isEqualTo("Manni");
    }

    @Test
    void cashCardDeserializationTest() throws IOException {
        String content = """
                {
                  "id": 99,
                  "amount": 123.45,
                  "owner": "Manni"
                }
                """;
        assertThat(json.parse(content)).isEqualTo(new CashCard(99L, 123.45, "Manni"));
        assertThat(json.parseObject(content).id()).isEqualTo(99L);
        assertThat(json.parseObject(content).amount()).isEqualTo(123.45);
        assertThat(json.parseObject(content).owner()).isEqualTo("Manni");
    }

    @Test
    void cashCardListSerializationTest() throws IOException {
        assertThat(jsonList.write(cashCards)).isStrictlyEqualToJson("list.json");
    }

    @Test
    void cashCardListDeserializationTest() throws IOException {
        String content = """
                [
                  { "id": 99, "amount": 123.45, "owner": "Manni" },
                  { "id": 100, "amount": 1.0, "owner": "Toni" },
                  { "id": 101, "amount": 150.0, "owner": "Trish" }
                ]
                """;
        assertThat(jsonList.parse(content)).isEqualTo(cashCards);
    }

}
