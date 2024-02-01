package ru.neoflex.scammertracking.analyzer.config;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.neoflex.scammertracking.analyzer.error.decoder.RetreiveMessageErrorDecoder;

@Configuration
public class DecoderConfiguration {

    @Bean
    public ErrorDecoder errorDecoder() {
        return new RetreiveMessageErrorDecoder();
    }
}
