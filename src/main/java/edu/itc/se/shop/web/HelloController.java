package edu.itc.se.shop.web;

import java.time.Instant;
import org.springframework.web.bind.annotation.*;

@RestController
public class HelloController {

    public record Greeting(String message, Instant at) {}

    @GetMapping("/hello")
    public Greeting hello(@RequestParam(defaultValue = "World") String name) {
        String message = String.format("Hello, %s! Man in black", name);
        return new Greeting(message, Instant.now());
    }
}
