package ru.neoflex.scammertracking.analyzer.config;

import feign.codec.ErrorDecoder;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.neoflex.scammertracking.analyzer.error.decoder.RetreiveMessageErrorDecoder;

@Configuration
public class AnalyzerConfig {

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }
}
