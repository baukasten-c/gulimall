package com.atguigu.common.to;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SkuHasStockTo {
    private Long skuId;
    private boolean hasStock;
}
