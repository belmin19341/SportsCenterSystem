package ba.nwt.paymentservice.dto;

import ba.nwt.paymentservice.model.Document;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentRequestDTO {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Related entity ID is required")
    private Long relatedEntityId;

    @NotNull(message = "Related entity type is required")
    private Document.RelatedEntityType relatedEntityType;

    @NotNull(message = "Document type is required")
    private Document.DocumentType documentType;

    @Size(max = 500, message = "File path must be at most 500 characters")
    private String filePath;
}

