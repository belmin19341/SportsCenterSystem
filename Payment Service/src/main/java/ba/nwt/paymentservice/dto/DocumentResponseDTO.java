package ba.nwt.paymentservice.dto;

import ba.nwt.paymentservice.model.Document;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentResponseDTO {
    private Long id;
    private Long userId;
    private Long relatedEntityId;
    private Document.RelatedEntityType relatedEntityType;
    private Document.DocumentType documentType;
    private String filePath;
    private LocalDateTime generatedAt;
}

