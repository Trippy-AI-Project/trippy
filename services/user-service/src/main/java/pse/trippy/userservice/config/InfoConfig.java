package pse.trippy.userservice.config;

import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.actuate.info.Info;

@Configuration
public class InfoConfig {
    @Bean
    public InfoContributor customInfoContributor(BuildProperties buildProperties) {
        return (Info.Builder builder) -> builder
            .withDetail("service", "user-service")
            .withDetail("version", buildProperties.getVersion())
            .withDetail("git", buildProperties.get("git.commit.id"));
    }
}package pse.trippy.userservice.config;

import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.actuate.info.Info;

@Configuration
public class InfoConfig {
    @Bean
    public InfoContributor customInfoContributor(BuildProperties buildProperties) {
        return (Info.Builder builder) -> builder
            .withDetail("service", "user-service")
            .withDetail("version", buildProperties.getVersion())
            .withDetail("git", buildProperties.get("git.commit.id"));
    }
}
