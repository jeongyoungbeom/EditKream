package com.project.kream.Model.request;

import com.project.kream.Model.Entity.Customer;
import com.project.kream.Model.Entity.Product;
import com.project.kream.Model.Entity.Style;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class StyleApiRequest {
    private Long id;
    private Long customerId;
    private String content;
    private String[] tagName;
    private Long[] ProductId;
    private Long hit;

    public Style toEntity(Customer customer){
        return Style.builder()
                .id(id)
                .customer(customer)
                .content(content)
                .hit(hit)
                .build();
    }
}
