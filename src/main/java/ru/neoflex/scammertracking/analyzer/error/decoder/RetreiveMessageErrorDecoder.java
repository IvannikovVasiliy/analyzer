package ru.neoflex.scammertracking.analyzer.error.decoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import ru.neoflex.scammertracking.analyzer.domain.dto.MessageInfoDto;
import ru.neoflex.scammertracking.analyzer.error.exception.BadRequestException;
import ru.neoflex.scammertracking.analyzer.error.exception.NotFoundException;
import ru.neoflex.scammertracking.analyzer.error.valid.ValidationErrorResponse;

import java.io.IOException;

public class RetreiveMessageErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder errorDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        byte[] responseBytes = null;
        try {
             responseBytes = response.body().asInputStream().readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }

        String error = null;
        ObjectMapper mapper = new ObjectMapper();
        try {
            MessageInfoDto message = mapper.readValue(responseBytes, MessageInfoDto.class);
            error = message.getMessage();
        } catch (Exception e) {
            try {
                ValidationErrorResponse errorResponse = mapper.readValue(responseBytes, ValidationErrorResponse.class);
                StringBuffer buffer = new StringBuffer();
                errorResponse.getViolations()
                        .forEach(violation -> {
                            String violationString = String.format("The field: %s has error: %s;", violation.getFieldName(), violation.getMessage());
                            buffer.append(violationString);
                        });
                error = buffer.toString();
            } catch (IOException ex) {
                throw new RuntimeException(e.getMessage());
            }
        }

        switch (response.status()) {
            case 400:
                return new BadRequestException(null != error ? error : "Bad request");
            case 404:
                return new NotFoundException(null != error ? error : "Not Found");
            default:
                return errorDecoder.decode(methodKey, response);
        }
    }
}