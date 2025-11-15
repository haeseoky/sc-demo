package com.ocean.scdemo.aop.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 주문 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    private String userId;
    private String orderId;
    private String productId;
    private Integer quantity;
    private Double amount;

}
