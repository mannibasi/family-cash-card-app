package org.familycashcardapp;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FamilyCashCardAppApplicationTests {

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void shouldReturnACashCardWhenDataIsSaved() {
        ResponseEntity<String> response = restTemplate.withBasicAuth("manni", "testUserPassword123!").getForEntity("/cashcards/99", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        DocumentContext documentContext = JsonPath.parse(response.getBody());
        Number id = documentContext.read("$.id");
        assertThat(id).isEqualTo(99);

        Double amount = documentContext.read("$.amount");
        assertThat(amount).isEqualTo(123.45);

        String owner = documentContext.read("$.owner");
        assertThat(owner).isEqualTo("Manni");
    }

    @Test
    void shouldNotReturnACashCardWithUnknownId() {
        ResponseEntity<String> response = restTemplate.withBasicAuth("manni", "testUserPassword123!").getForEntity("/cashcards/1000", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isBlank();
    }

    @Test
    @DirtiesContext
    void shouldCreateANewCashCard() {
        CashCard newCashCard = new CashCard(null, 250.00, null);
        ResponseEntity<Void> createResponse = restTemplate.withBasicAuth("manni", "testUserPassword123!").postForEntity("/cashcards", newCashCard, Void.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        URI locationOfNewCashCard = createResponse.getHeaders().getLocation();
        ResponseEntity<String> getResponse = restTemplate.withBasicAuth("manni", "testUserPassword123!").getForEntity(locationOfNewCashCard, String.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldReturnAllCashCardsWhenListIsRequested() {
        ResponseEntity<String> response = restTemplate.withBasicAuth("manni", "testUserPassword123!").getForEntity("/cashcards", String.class);

        DocumentContext documentContext = JsonPath.parse(response.getBody());
        int cashCardCount = documentContext.read("$.length()");
        assertThat(cashCardCount).isEqualTo(3);

        JSONArray ids = documentContext.read("$..id");
        assertThat(ids).containsExactlyInAnyOrder(99, 100, 101);

        JSONArray amounts = documentContext.read("$..amount");
        assertThat(amounts).containsExactlyInAnyOrder(123.45, 1.0, 150.00);

        JSONArray owners = documentContext.read("$..owner");
        assertThat(owners).containsExactlyInAnyOrder("Manni", "Manni", "Manni");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldReturnAPageOfCashCards() {
        ResponseEntity<String> response = restTemplate.withBasicAuth("manni", "testUserPassword123!").getForEntity("/cashcards?page=0&size=1", String.class);

        DocumentContext documentContext = JsonPath.parse(response.getBody());

        JSONArray page = documentContext.read("$[*]");
        assertThat(page.size()).isEqualTo(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldReturnASortedPageOfCashCards() {
        ResponseEntity<String> response = restTemplate.withBasicAuth("manni", "testUserPassword123!").getForEntity("/cashcards?page=0&size=1&sort=amount,desc", String.class);

        DocumentContext documentContext = JsonPath.parse(response.getBody());

        JSONArray read = documentContext.read("$[*]");
        assertThat(read.size()).isEqualTo(1);

        double amount = documentContext.read("$[0].amount");
        assertThat(amount).isEqualTo(150.00);
    }

    @Test
    void shouldReturnASortedPageOfCashCardsWithNoParametersAndUseDefaultValues() {
        ResponseEntity<String> response = restTemplate.withBasicAuth("manni", "testUserPassword123!").getForEntity("/cashcards", String.class);

        DocumentContext documentContext = JsonPath.parse(response.getBody());

        JSONArray read = documentContext.read("$[*]");
        assertThat(read.size()).isEqualTo(3);

        JSONArray amounts = documentContext.read("$..amount");
        assertThat(amounts).containsExactly(1.00, 123.45, 150.00);
    }

    @Test
    void shouldNotReturnACashCardWhenUsingBadPasswordCredentials() {
        ResponseEntity<String> response = restTemplate.withBasicAuth("manni", "incorrectTestUserPassword123!").getForEntity("/cashcards/99", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldNotReturnACashCardWhenUsingBadUserCredentials() {
        ResponseEntity<String> response = restTemplate.withBasicAuth("incorrectUser", "testUserPassword123!").getForEntity("/cashcards/99", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldRejectUsersWhoAreNotCardOwners() {
        ResponseEntity<String> response = restTemplate.withBasicAuth("Hank", "someOtherTestUserPassword123!").getForEntity("/cashcards/99", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldNotAllowAccessToCashCardsTheyDoNotOwn() {
        //102 is owned by Dave
        ResponseEntity<String> response = restTemplate.withBasicAuth("Manni", "testUserPassword123!").getForEntity("/cashcards/102", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DirtiesContext
    void shouldUpdateAnExistingCashCard() {
        CashCard cashCardUpdate = new CashCard(null, 249.95, null);
        HttpEntity<CashCard> request = new HttpEntity<>(cashCardUpdate);
        ResponseEntity<Void> response = restTemplate.withBasicAuth("manni", "testUserPassword123!").exchange("/cashcards/99", HttpMethod.PUT, request, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> getResponse = restTemplate.withBasicAuth("manni", "testUserPassword123!").getForEntity("/cashcards/99", String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        DocumentContext documentContext = JsonPath.parse(getResponse.getBody());
        Number id = documentContext.read("$.id");
        Double amount = documentContext.read("$.amount");
        assertThat(id).isEqualTo(99);
        assertThat(amount).isEqualTo(249.95);
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingNonExistentCashCard() {
        CashCard unknownCashCardUpdate = new CashCard(null, 123.45, null);
        HttpEntity<CashCard> request = new HttpEntity<>(unknownCashCardUpdate);
        ResponseEntity<Void> response = restTemplate.withBasicAuth("manni", "testUserPassword123!").exchange("/cashcards/1000", HttpMethod.PUT, request, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldNotUpdateACashCardThatIsOwnedBySomeoneElse() {
        CashCard davesCard = new CashCard(null, 12.34, null);
        HttpEntity<CashCard> request = new HttpEntity<>(davesCard);
        ResponseEntity<Void> response = restTemplate.withBasicAuth("manni", "testUserPassword123!").exchange("/cashcards/102", HttpMethod.PUT, request, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DirtiesContext
    void shouldDeleteAnExistingCashCard() {
        ResponseEntity<Void> response = restTemplate.withBasicAuth("manni", "testUserPassword123!").exchange("/cashcards/99", HttpMethod.DELETE, null, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> getResponse = restTemplate.withBasicAuth("manni", "testUserPassword123!").getForEntity("/cashcards/99", String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldNotDeleteACashCardThatDoesNotExist() {
        ResponseEntity<Void> response = restTemplate.withBasicAuth("manni", "testUserPassword123!").exchange("/cashcards/1000", HttpMethod.DELETE, null, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldNotDeleteACashCardThatIsOwnedBySomeoneElse() {
        ResponseEntity<Void> response = restTemplate.withBasicAuth("manni", "testUserPassword123!").exchange("/cashcards/102", HttpMethod.DELETE, null, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<String> getResponse = restTemplate.withBasicAuth("dave", "testAnotherUserPassword123!").getForEntity("/cashcards/102", String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
