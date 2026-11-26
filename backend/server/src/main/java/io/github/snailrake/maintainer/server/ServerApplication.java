package io.github.snailrake.maintainer.server;

import io.github.snailrake.maintainer.server.service.AccountService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@Slf4j
@SpringBootApplication
public class ServerApplication {

    private static final String RECOVER_ADMIN_OPTION = "recover-admin";

    public static void main(String[] args) {
        var application = new SpringApplication(ServerApplication.class);
        var recoverAdmin = new DefaultApplicationArguments(args).containsOption(RECOVER_ADMIN_OPTION);
        if (recoverAdmin) {
            application.setWebApplicationType(WebApplicationType.NONE);
        }

        var context = application.run(args);
        if (recoverAdmin) {
            context.close();
        }
    }

    @Bean
    ApplicationRunner administratorInitializer(AccountService accountService, Environment environment) {
        return args -> {
            var login = environment.getProperty("maintainer.admin.login");
            var password = environment.getProperty("maintainer.admin.password");
            if (args.containsOption(RECOVER_ADMIN_OPTION)) {
                var administratorId = accountService.recoverAdministrator(login, password);
                log.info("Восстановлен административный доступ: userId={}", administratorId);
            } else {
                var administratorId = accountService.initializeAdministrator(login, password);
                administratorId.ifPresent(id -> log.info("Создан первый администратор: userId={}", id));
            }
        };
    }
}
