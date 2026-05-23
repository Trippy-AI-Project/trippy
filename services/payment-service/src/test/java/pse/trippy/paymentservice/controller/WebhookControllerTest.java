package pse.trippy.paymentservice.controller;

import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pse.trippy.paymentservice.service.WebhookService;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.net.Webhook;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("WebhookController")
class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WebhookService webhookService;

    @Test
    @DisplayName("POST /payments/webhook returns 200 for valid checkout.session.completed")
    void webhookHandlesCheckoutSessionCompleted() throws Exception {
        Event event = mock(Event.class);
        when(event.getType()).thenReturn("checkout.session.completed");
        when(event.getId()).thenReturn("evt_test_123");

        Session session = mock(Session.class);
        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.of(session));

        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            webhookMock.when(() -> Webhook.constructEvent(any(), any(), any())).thenReturn(event);

            mockMvc.perform(post("/payments/webhook")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}")
                            .header("Stripe-Signature", "t=123,v1=abc"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Received"));

            verify(webhookService).handleCheckoutSessionCompleted(session);
        }
    }

    @Test
    @DisplayName("POST /payments/webhook returns 400 for invalid signature")
    void webhookRejectsInvalidSignature() throws Exception {
        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            webhookMock.when(() -> Webhook.constructEvent(any(), any(), any()))
                    .thenThrow(new com.stripe.exception.SignatureVerificationException(
                            "Invalid signature", "sig_header"));

            mockMvc.perform(post("/payments/webhook")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}")
                            .header("Stripe-Signature", "invalid"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Invalid signature"));
        }
    }
}
