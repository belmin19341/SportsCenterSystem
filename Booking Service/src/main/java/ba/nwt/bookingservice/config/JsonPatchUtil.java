package ba.nwt.bookingservice.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JsonPatchUtil {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public <T> T apply(JsonPatch patch, T target, Class<T> type) {
        try {
            JsonNode patched = patch.apply(objectMapper.convertValue(target, JsonNode.class));
            T result = objectMapper.treeToValue(patched, type);
            Set<ConstraintViolation<T>> violations = validator.validate(result);
            if (!violations.isEmpty()) {
                String details = violations.stream()
                        .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                        .collect(Collectors.joining(", "));
                throw new IllegalArgumentException("Patch produced invalid state: " + details);
            }
            return result;
        } catch (JsonPatchException e) {
            throw new IllegalArgumentException("Invalid JSON Patch: " + e.getMessage());
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to apply JSON Patch: " + e.getMessage());
        }
    }
}
