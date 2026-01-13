package com.banking.platform.accounts.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "customers_summary") // Nombre explícito para denotar que es una vista/resumen
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomerProjection {

    @Id
    private Long id; // NO es autogenerado. Usamos el mismo ID que viene de ms-customers

    private String fullName;
    private String email;
}