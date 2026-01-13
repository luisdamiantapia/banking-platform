package com.banking.platform.customers.service;

import com.banking.platform.customers.config.KafkaConfig;
import com.banking.platform.customers.entity.Customer;
import com.banking.platform.customers.events.CustomerCreatedEvent;
import com.banking.platform.customers.repository.CustomerRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private static Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public Customer createCustomer(Customer customer) {
        // 1. Guardar en Base de Datos (Postgres)
        customer.setActive(true);
        Customer savedCustomer = customerRepository.save(customer);
        log.info("Cliente guardado en DB con ID: {}", savedCustomer.getId());

        // 2. Crear el evento
        CustomerCreatedEvent event = CustomerCreatedEvent.builder()
                .id(savedCustomer.getId())
                .fullName(savedCustomer.getFullName())
                .email(savedCustomer.getEmail())
                .build();

        // 3. Publicar en Kafka (Asíncrono)
        // Usamos el ID del cliente como KEY para garantizar orden (si hubiera particiones)
        kafkaTemplate.send(KafkaConfig.CUSTOMER_TOPIC, savedCustomer.getId().toString(), event);

        log.info("Evento CustomerCreatedEvent enviado a Kafka para el ID: {}", savedCustomer.getId());

        return savedCustomer;
    }
}