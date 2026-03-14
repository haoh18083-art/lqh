package com.campusmedical.module.appointment.service;

import com.campusmedical.common.exception.NotFoundException;
import com.campusmedical.infrastructure.persistence.mysql.entity.AppointmentEntity;
import com.campusmedical.infrastructure.persistence.mysql.entity.ConsultationEntity;
import com.campusmedical.infrastructure.persistence.mysql.entity.DocumentEntity;
import com.campusmedical.infrastructure.persistence.mysql.entity.PrescriptionItemEntity;
import com.campusmedical.infrastructure.persistence.mysql.repository.DocumentRepository;
import com.campusmedical.module.appointment.dto.AppointmentDtos;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class AppointmentDocumentService {

    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final DocumentRepository documentRepository;
    private final String documentStoragePath;

    public AppointmentDocumentService(
        DocumentRepository documentRepository,
        @Value("${app.storage.document-path}") String documentStoragePath
    ) {
        this.documentRepository = documentRepository;
        this.documentStoragePath = documentStoragePath;
    }

    public List<AppointmentDtos.DocumentInfo> listDocuments(Long appointmentId) {
        List<AppointmentDtos.DocumentInfo> items = new ArrayList<AppointmentDtos.DocumentInfo>();
        for (DocumentEntity entity : documentRepository.findByAppointment_IdOrderByCreatedAtDesc(appointmentId)) {
            AppointmentDtos.DocumentInfo item = new AppointmentDtos.DocumentInfo();
            item.setId(entity.getId());
            item.setAppointmentId(entity.getAppointment().getId());
            item.setDocType(entity.getDocType());
            item.setFileName(entity.getFileName());
            item.setCreatedAt(entity.getCreatedAt());
            items.add(item);
        }
        return items;
    }

    public DocumentEntity getDocumentEntity(Long appointmentId, String docType) {
        return documentRepository
            .findFirstByAppointment_IdAndDocTypeOrderByCreatedAtDesc(appointmentId, docType)
            .orElseThrow(() -> new NotFoundException("文档不存在"));
    }

    public Resource loadAsResource(DocumentEntity document) {
        Path path = Paths.get(document.getFilePath());
        if (!Files.exists(path)) {
            throw new NotFoundException("文档文件不存在");
        }
        return new FileSystemResource(path);
    }

    public DocumentEntity createDiagnosisDocument(AppointmentEntity appointment, ConsultationEntity consultation) {
        List<String> lines = new ArrayList<String>();
        lines.add("Appointment ID: " + appointment.getId());
        lines.add("Visit Time: " + appointment.getVisitDate() + " " + safeText(appointment.getTimeSlot()));
        lines.add("Department: " + safeText(resolveDepartmentName(appointment)));
        lines.add("Doctor: " + safeText(resolveDoctorName(appointment)));
        lines.add("Student: " + safeText(resolveStudentName(appointment)));
        lines.add("Signs: " + safeText(consultation.getSigns()));
        lines.add("Conclusion: " + safeText(consultation.getConclusion()));
        lines.add("Advice: " + safeText(consultation.getAdvice()));
        return createAndPersistDocument(appointment, "diagnosis", "Diagnosis Report", lines);
    }

    public DocumentEntity createPrescriptionDocument(AppointmentEntity appointment, List<PrescriptionItemEntity> items) {
        List<String> lines = new ArrayList<String>();
        lines.add("Appointment ID: " + appointment.getId());
        lines.add("Visit Time: " + appointment.getVisitDate() + " " + safeText(appointment.getTimeSlot()));
        lines.add("Department: " + safeText(resolveDepartmentName(appointment)));
        lines.add("Doctor: " + safeText(resolveDoctorName(appointment)));
        lines.add("Student: " + safeText(resolveStudentName(appointment)));
        if (items == null || items.isEmpty()) {
            lines.add("Prescription Items: None");
        } else {
            lines.add("Prescription Items:");
            int index = 1;
            for (PrescriptionItemEntity item : items) {
                lines.add(
                    index + ". " + safeText(item.getName()) + " x " + item.getQuantity() +
                    (item.getUnit() == null ? "" : safeText(item.getUnit())) +
                    " dosage=" + safeText(item.getDosage())
                );
                index += 1;
            }
        }
        return createAndPersistDocument(appointment, "prescription", "Prescription Report", lines);
    }

    private DocumentEntity createAndPersistDocument(
        AppointmentEntity appointment,
        String docType,
        String title,
        List<String> lines
    ) {
        String fileName = docType + "_" + appointment.getId() + "_" + timestamp() + ".pdf";
        Path filePath = resolveStorageDir().resolve(fileName);
        writePdf(filePath, title, lines);

        DocumentEntity document = new DocumentEntity();
        document.setAppointment(appointment);
        document.setDocType(docType);
        document.setFileName(fileName);
        document.setFilePath(filePath.toString());
        return documentRepository.save(document);
    }

    private void writePdf(Path path, String title, List<String> lines) {
        try {
            Files.createDirectories(path.getParent());
            PDDocument document = new PDDocument();
            try {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                PDPageContentStream stream = new PDPageContentStream(document, page);
                try {
                    float y = 800F;
                    stream.beginText();
                    stream.setFont(PDType1Font.HELVETICA_BOLD, 16F);
                    stream.newLineAtOffset(40F, y);
                    stream.showText(safeText(title));
                    stream.endText();

                    y -= 30F;
                    for (String line : lines) {
                        if (y < 60F) {
                            stream.close();
                            page = new PDPage(PDRectangle.A4);
                            document.addPage(page);
                            stream = new PDPageContentStream(document, page);
                            y = 800F;
                        }
                        stream.beginText();
                        stream.setFont(PDType1Font.HELVETICA, 11F);
                        stream.newLineAtOffset(40F, y);
                        stream.showText(safeText(line));
                        stream.endText();
                        y -= 18F;
                    }
                } finally {
                    stream.close();
                }
                document.save(path.toFile());
            } finally {
                document.close();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("生成 PDF 文档失败", exception);
        }
    }

    private Path resolveStorageDir() {
        Path path = Paths.get(documentStoragePath);
        if (!path.isAbsolute()) {
            path = path.toAbsolutePath().normalize();
        }
        return path;
    }

    private String timestamp() {
        return LocalDateTime.now(ZoneOffset.UTC).format(FILE_TIME_FORMATTER);
    }

    private String resolveDoctorName(AppointmentEntity appointment) {
        if (appointment.getDoctor() == null || appointment.getDoctor().getUser() == null) {
            return "Unknown";
        }
        return appointment.getDoctor().getUser().getFullName();
    }

    private String resolveStudentName(AppointmentEntity appointment) {
        if (appointment.getStudent() == null || appointment.getStudent().getUser() == null) {
            return "Unknown";
        }
        return appointment.getStudent().getUser().getFullName();
    }

    private String resolveDepartmentName(AppointmentEntity appointment) {
        if (appointment.getDepartment() != null) {
            return appointment.getDepartment().getName();
        }
        if (appointment.getDoctor() != null && appointment.getDoctor().getDepartmentRel() != null) {
            return appointment.getDoctor().getDepartmentRel().getName();
        }
        return "Unknown";
    }

    private String safeText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "N/A";
        }
        StringBuilder builder = new StringBuilder();
        char[] chars = value.trim().toCharArray();
        for (char ch : chars) {
            if (ch >= 32 && ch <= 126) {
                builder.append(ch);
            } else {
                builder.append('?');
            }
        }
        return builder.toString();
    }
}
