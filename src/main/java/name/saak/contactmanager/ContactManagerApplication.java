package name.saak.contactmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ContactManagerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ContactManagerApplication.class, args);
    }
}
