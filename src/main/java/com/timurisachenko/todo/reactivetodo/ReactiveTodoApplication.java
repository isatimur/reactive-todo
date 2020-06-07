package com.timurisachenko.todo.reactivetodo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.stream.Stream;

@SpringBootApplication
public class ReactiveTodoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReactiveTodoApplication.class, args);
    }

    @Bean
    ApplicationRunner init(TodoRepository repository, DatabaseClient client) {
        return args -> {
            client.execute("create table IF NOT EXISTS TODO" +
                    "(id SERIAL PRIMARY KEY, text varchar (255) not null, completed boolean default false);").fetch().first().subscribe();
            client.execute("DELETE FROM TODO;").fetch().first().subscribe();

            Stream<Todo> stream = Stream.of(new Todo(null, "Hi this is my first todo!", false),
                    new Todo(null, "This one I have acomplished!", true),
                    new Todo(null, "And this is secret", false));

            // initialize the database

            repository.saveAll(Flux.fromStream(stream))
                    .then()
                    .subscribe(); // execute

        };
    }

}


@RestController
@RequestMapping("/api")
class TodoController {
    private TodoRepository repository;

    @Autowired
    public TodoController(TodoRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/todo")
    Flux<Todo> getAll() {
        return repository.findAll();
    }

    @PostMapping("/todo")
    Mono<Todo> addTodo(@RequestBody Todo todo) {
        return repository.save(todo);
    }

    @PutMapping("/todo")
    Mono<Todo> updateTodo(@RequestBody Todo todo) {
        return repository.save(todo);
    }

    @DeleteMapping("/todo/{id}")
    Mono<Void> deleteById(@PathVariable("id") Long id) {
        return repository.deleteById(id);
    }
}

@Table
class Todo {
    @Id
    private Long id;
    private String text;
    private boolean completed;

    public Todo(Long id, String text, boolean completed) {
        this.id = id;
        this.text = text;
        this.completed = completed;
    }

    public Todo() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Todo)) return false;

        Todo todo = (Todo) o;

        if (isCompleted() != todo.isCompleted()) return false;
        if (getId() != null ? !getId().equals(todo.getId()) : todo.getId() != null) return false;
        return getText() != null ? getText().equals(todo.getText()) : todo.getText() == null;
    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getText() != null ? getText().hashCode() : 0);
        result = 31 * result + (isCompleted() ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Todo{" +
                "id=" + id +
                ", text='" + text + '\'' +
                ", completed=" + completed +
                '}';
    }
}

interface TodoRepository extends ReactiveCrudRepository<Todo, Long> {

}

@ResponseStatus(HttpStatus.NOT_FOUND)
class TodoNotFoundException extends RuntimeException {

    public TodoNotFoundException(String todoId) {
        super("could not find todo '" + todoId + "'.");
    }
}