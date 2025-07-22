package com.moola.fx.moneychanger.rate.strategy;

import com.moola.fx.moneychanger.rate.dto.BasicRate;
import com.moola.fx.moneychanger.rate.dto.ComputedRate;

import java.math.BigDecimal;
import java.util.List;

public class StandardFormula implements RateCalculationStrategy {
    @Override
    public List<ComputedRate> compute(List<BasicRate> basicRates) {
        return basicRates.stream()
                .map(this::computeSingle)
                .toList();
    }

    private ComputedRate computeSingle(BasicRate basicRate) {
        ComputedRate result = new ComputedRate();

        result.setCurrencyCode(basicRate.getCurrencyCode());
        result.setUnit(basicRate.getUnit());
        result.setRawBid(basicRate.getRawBid());
        result.setRawAsk(basicRate.getRawAsk());

        BigDecimal spread = basicRate.getRawAsk().subtract(basicRate.getRawBid());
        result.setSpread(spread);

        BigDecimal skew = spread.multiply(BigDecimal.valueOf(0.1));
        result.setSkew(skew);

        result.setWsBid(basicRate.getRawBid().subtract(skew));
        result.setWsAsk(basicRate.getRawAsk().add(skew));

        result.setRefBid(result.getWsBid());
        result.setRefAsk(result.getWsAsk());

        return result;
    }
}
