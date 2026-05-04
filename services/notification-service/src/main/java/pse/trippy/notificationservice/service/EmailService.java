package pse.trippy.notificationservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import pse.trippy.notificationservice.model.entity.EmailLog;
import pse.trippy.notificationservice.model.enums.EmailStatus;
import pse.trippy.notificationservice.repository.EmailLogRepository;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final EmailLogRepository emailLogRepository;

    @Async("emailExecutor")
    public void sendTemplateEmail(String to, String subject, String templateName,
                                  Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);

        String htmlBody = templateEngine.process("email/" + templateName, context);
        sendHtmlEmail(to, subject, templateName, htmlBody);
    }

    public String renderTemplate(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        return templateEngine.process("email/" + templateName, context);
    }

    private void sendHtmlEmail(String to, String subject, String templateName, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(toPlainText(htmlBody), htmlBody);

            mailSender.send(message);

            emailLogRepository.save(EmailLog.builder()
                    .recipient(to)
                    .subject(subject)
                    .templateName(templateName)
                    .status(EmailStatus.SENT)
                    .build());

            log.info("Email sent to {} with template {}", to, templateName);

        } catch (MessagingException e) {
            log.error("Failed to send email to {} with template {}: {}",
                    to, templateName, e.getMessage());

            emailLogRepository.save(EmailLog.builder()
                    .recipient(to)
                    .subject(subject)
                    .templateName(templateName)
                    .status(EmailStatus.FAILED)
                    .errorMessage(e.getMessage())
                    .build());
        }
    }

    private String toPlainText(String htmlBody) {
        if (htmlBody == null || htmlBody.isBlank()) {
            return "";
        }
        return htmlBody
                .replaceAll("(?is)<style.*?</style>", " ")
                .replaceAll("(?is)<script.*?</script>", " ")
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n\n")
                .replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll("\\n\\s+", "\n")
                .trim();
    }
}
