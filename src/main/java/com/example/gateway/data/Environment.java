package com.example.gateway.data;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class Environment implements Serializable {

    Integer temperature;
    Integer humidity;
    Integer co2;

}