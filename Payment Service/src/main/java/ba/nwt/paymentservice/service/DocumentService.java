package ba.nwt.paymentservice.service;

import ba.nwt.paymentservice.dto.DocumentRequestDTO;
import ba.nwt.paymentservice.dto.DocumentResponseDTO;
import ba.nwt.paymentservice.exception.ResourceNotFoundException;
import ba.nwt.paymentservice.model.Document;
import ba.nwt.paymentservice.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ModelMapper modelMapper;

    public List<DocumentResponseDTO> getAll() {
        return documentRepository.findAll().stream()
                .map(d -> modelMapper.map(d, DocumentResponseDTO.class))
                .collect(Collectors.toList());
    }

    public DocumentResponseDTO getById(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + id));
        return modelMapper.map(document, DocumentResponseDTO.class);
    }

    public List<DocumentResponseDTO> getByUserId(Long userId) {
        return documentRepository.findByUserId(userId).stream()
                .map(d -> modelMapper.map(d, DocumentResponseDTO.class))
                .collect(Collectors.toList());
    }

    public DocumentResponseDTO create(DocumentRequestDTO dto) {
        Document document = Document.builder()
                .userId(dto.getUserId())
                .relatedEntityId(dto.getRelatedEntityId())
                .relatedEntityType(dto.getRelatedEntityType())
                .documentType(dto.getDocumentType())
                .filePath(dto.getFilePath())
                .build();

        Document saved = documentRepository.save(document);
        return modelMapper.map(saved, DocumentResponseDTO.class);
    }

    public void delete(Long id) {
        if (!documentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Document not found with id: " + id);
        }
        documentRepository.deleteById(id);
    }
}

