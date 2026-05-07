package pse.trippy.notificationservice.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pse.trippy.notificationservice.service.EmailService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailTemplatePreviewController")
class EmailTemplatePreviewControllerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmailTemplatePreviewController controller;

    @Test
    @DisplayName("preview renders allowlisted template")
    void previewRendersAllowlistedTemplate() {
        when(emailService.renderTemplate(eq("welcome"), anyMap())).thenReturn("<html>Welcome</html>");

        ResponseEntity<String> response = controller.preview("welcome");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("<html>Welcome</html>");
    }

    @Test
    @DisplayName("preview rejects non-allowlisted template")
    void previewRejectsNonAllowlistedTemplate() {
        ResponseEntity<String> response = controller.preview("../welcome");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verifyNoInteractions(emailService);
    }
}
