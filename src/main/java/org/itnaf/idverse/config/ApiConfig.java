package org.itnaf.idverse.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@Slf4j
public class ApiConfig {

    private final Dotenv dotenv;

    public ApiConfig() {
        this.dotenv = Dotenv.configure()
                .filename(".env")
                .ignoreIfMissing()
                .load();
    }

    @Bean
    public Dotenv dotenv() {
        return this.dotenv;
    }

    @PostConstruct
    public void configureLogging() {
        String verboseLevel = dotenv.get("VERBOSE");
        if (verboseLevel == null || verboseLevel.isEmpty()) {
            verboseLevel = "DEBUG"; // Default to DEBUG
        }

        Level logLevel;
        try {
            logLevel = Level.toLevel(verboseLevel, Level.DEBUG);
        } catch (Exception e) {
            log.warn("Invalid VERBOSE level '{}', defaulting to DEBUG", verboseLevel);
            logLevel = Level.DEBUG;
        }

        // Set log level for the application package
        Logger rootLogger = (Logger) LoggerFactory.getLogger("org.itnaf.idverse");
        rootLogger.setLevel(logLevel);

        log.info("Application log level set to: {}", logLevel);

        // Set default values as system properties for @Value injection
        setSystemPropertyIfPresent("default.phone.code", dotenv.get("PHONE_CODE"));
        setSystemPropertyIfPresent("default.phone.number", dotenv.get("PHONE_NUMBER"));
        setSystemPropertyIfPresent("default.reference.id", dotenv.get("REFERENCE_ID"));
        setSystemPropertyIfPresent("default.transaction", dotenv.get("TRANSACTION"));
        setSystemPropertyIfPresent("default.name", dotenv.get("NAME"));
        setSystemPropertyIfPresent("default.supplied.first.name", dotenv.get("SUPPLIED_FIRST_NAME"));
    }

    private void setSystemPropertyIfPresent(String key, String value) {
        if (value != null && !value.isEmpty()) {
            System.setProperty(key, value);
        }
    }

    @Bean
    public String idverseClientId() {
        String clientId = dotenv.get("IDVERSE_CLIENT_ID");
        if (clientId == null || clientId.equals("your_client_id_here")) {
            log.warn("IDVERSE_CLIENT_ID not configured - running in DEMO MODE. API calls will fail.");
            return "demo_client_id";
        }
        return clientId;
    }

    @Bean
    public String idverseClientSecret() {
        String clientSecret = dotenv.get("IDVERSE_CLIENT_SECRET");
        if (clientSecret == null || clientSecret.equals("your_client_secret_here")) {
            log.warn("IDVERSE_CLIENT_SECRET not configured - running in DEMO MODE. API calls will fail.");
            return "demo_client_secret";
        }
        return clientSecret;
    }

    @Bean
    public String idverseOAuthUrl() {
        String oauthUrl = dotenv.get("IDVERSE_OAUTH_URL");
        if (oauthUrl == null) {
            oauthUrl = "https://usdemo.idkit.co/api/3.5/oauthToken";
        }
        return oauthUrl;
    }

    @Bean
    public String idverseApiUrl() {
        String apiUrl = dotenv.get("IDVERSE_API_URL");
        if (apiUrl == null) {
            apiUrl = "https://usdemo.idkit.co/api/3.5/sendSms";
        }
        return apiUrl;
    }

    @Bean
    public String oauthToken() {
        String token = dotenv.get("OAUTHTOKEN");
        if (token == null || token.isEmpty()) {
            log.warn("OAUTHTOKEN not configured - mock OAuth endpoint will return error");
            return ""; // Return empty string instead of null to allow bean creation
        }
        return token;
    }

    @Bean
    public String verboseMode() {
        String verbose = dotenv.get("VERBOSE");
        return verbose != null ? verbose : "INFO"; // Default to INFO if not set
    }

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
