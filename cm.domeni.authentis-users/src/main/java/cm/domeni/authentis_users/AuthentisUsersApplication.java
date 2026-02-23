package cm.domeni.authentis_users;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AuthentisUsersApplication {

  public static void main(String[] args) {
    SpringApplication.run(AuthentisUsersApplication.class, args);
  }
}
