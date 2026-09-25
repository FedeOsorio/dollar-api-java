package com.dollarapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DolarApiResponse {
    private String moneda;
    private String casa;
    private String nombre;
    private Double compra;
    private Double venta;
    private Instant fechaActualizacion;
}
