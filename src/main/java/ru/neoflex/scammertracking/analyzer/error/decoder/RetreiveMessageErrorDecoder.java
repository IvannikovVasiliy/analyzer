package ru.neoflex.scammertracking.analyzer.error.decoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import ru.neoflex.scammertracking.analyzer.domain.dto.MessageInfoDto;
import ru.neoflex.scammertracking.analyzer.error.exception.NotFoundException;

import java.io.IOException;
import java.io.InputStream;

public class RetreiveMessageErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder errorDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        MessageInfoDto message = null;
        try (InputStream bodyIs = response.body().asInputStream()) {
            ObjectMapper mapper = new ObjectMapper();
            message = mapper.readValue(bodyIs, MessageInfoDto.class);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        switch (response.status()) {
            case 404:
                return new NotFoundException(message.getMessage() != null ? message.getMessage() : "Not Found");
            default:
                return errorDecoder.decode(methodKey, response);
        }
    }
}