package com.project.kream.Model.request;

import com.project.kream.Model.enumclass.CardFlag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CardInfoApiRequest {
    private Long id;
    private CardFlag cardFlag;
    private String cardCompany;
    private String cardNumber;
    private String expiration;
    private String birthdate;
    private String cardpw;
    private Long customerId;
}
