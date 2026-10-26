package com.pedidos360.orders.service;

import com.pedidos360.orders.config.RabbitMQConfig;
import com.pedidos360.orders.entity.Order;
import com.pedidos360.orders.entity.OrderItem;
import com.pedidos360.orders.entity.OrderStatus;
import com.pedidos360.orders.event.BaseEventEnvelope;
import com.pedidos360.orders.event.EmailPayload;
import com.pedidos360.orders.event.InvoicePayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OrderEventPublisher orderEventPublisher;

    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        sampleOrder = new Order("CLI-98234", "Sofía Morales", "sofia.morales@example.com");
        sampleOrder.setId(1024L);
        sampleOrder.setClienteDireccion("Av. Providencia 1234, Santiago");
        sampleOrder.setClienteRut("18.765.432-1");
        sampleOrder.setEstado(OrderStatus.CREADO);

        OrderItem item = new OrderItem(1L, "Café Grano Geisha 250g", 2, new BigDecimal("7500.00"));
        item.setId(100L);
        sampleOrder.addItem(item);
    }

    @Test
    @DisplayName("Debe publicar evento de correo y de factura al crear un pedido")
    @SuppressWarnings("unchecked")
    void shouldPublishOrderCreatedEvents() {
        orderEventPublisher.publishOrderCreatedEvents(sampleOrder);

        ArgumentCaptor<BaseEventEnvelope<?>> envelopeCaptor = ArgumentCaptor.forClass(BaseEventEnvelope.class);

        // Verifica que se enviaron 2 mensajes: uno a cmd.email y otro a cmd.invoice
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_CMD_DIRECT),
                eq(RabbitMQConfig.ROUTING_KEY_EMAIL),
                envelopeCaptor.capture()
        );

        BaseEventEnvelope<?> emailEnvelope = envelopeCaptor.getValue();
        assertThat(emailEnvelope).isNotNull();
        assertThat(emailEnvelope.getType()).isEqualTo("cmd.email.send");
        assertThat(emailEnvelope.getEventId()).isNotBlank();
        assertThat(emailEnvelope.getPayload()).isInstanceOf(EmailPayload.class);

        EmailPayload emailPayload = (EmailPayload) emailEnvelope.getPayload();
        assertThat(emailPayload.getTo()).isEqualTo("sofia.morales@example.com");
        assertThat(emailPayload.getOrderNumber()).isEqualTo("PED-1024");
        assertThat(emailPayload.getCustomerName()).isEqualTo("Sofía Morales");

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_CMD_DIRECT),
                eq(RabbitMQConfig.ROUTING_KEY_INVOICE),
                envelopeCaptor.capture()
        );

        BaseEventEnvelope<?> invoiceEnvelope = envelopeCaptor.getValue();
        assertThat(invoiceEnvelope).isNotNull();
        assertThat(invoiceEnvelope.getType()).isEqualTo("cmd.invoice.generate");
        assertThat(invoiceEnvelope.getEventId()).isNotBlank();
        assertThat(invoiceEnvelope.getPayload()).isInstanceOf(InvoicePayload.class);

        InvoicePayload invoicePayload = (InvoicePayload) invoiceEnvelope.getPayload();
        assertThat(invoicePayload.getOrderId()).isEqualTo("PED-1024");
        assertThat(invoicePayload.getCustomer().getName()).isEqualTo("Sofía Morales");
        assertThat(invoicePayload.getCustomer().getEmail()).isEqualTo("sofia.morales@example.com");
        assertThat(invoicePayload.getCustomer().getTaxId()).isEqualTo("18.765.432-1");
        assertThat(invoicePayload.getItems()).hasSize(1);
        assertThat(invoicePayload.getTotal()).isEqualByComparingTo("15000.00");
    }

    @Test
    @DisplayName("Debe publicar evento de actualización de estado hacia cmd.email")
    @SuppressWarnings("unchecked")
    void shouldPublishOrderStatusChangedEvent() {
        orderEventPublisher.publishOrderStatusChangedEvent(sampleOrder, OrderStatus.CREADO, OrderStatus.ACEPTADO);

        ArgumentCaptor<BaseEventEnvelope<?>> envelopeCaptor = ArgumentCaptor.forClass(BaseEventEnvelope.class);

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_CMD_DIRECT),
                eq(RabbitMQConfig.ROUTING_KEY_EMAIL),
                envelopeCaptor.capture()
        );

        BaseEventEnvelope<?> envelope = envelopeCaptor.getValue();
        assertThat(envelope).isNotNull();
        assertThat(envelope.getType()).isEqualTo("cmd.email.send");

        EmailPayload emailPayload = (EmailPayload) envelope.getPayload();
        assertThat(emailPayload.getTo()).isEqualTo("sofia.morales@example.com");
        assertThat(emailPayload.getNewStatus()).isEqualTo("ACEPTADO");
        assertThat(emailPayload.getOrderNumber()).isEqualTo("PED-1024");
    }
}
