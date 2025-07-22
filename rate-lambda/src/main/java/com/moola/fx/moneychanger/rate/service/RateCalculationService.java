package com.moola.fx.moneychanger.rate.service;
import com.moola.fx.moneychanger.rate.dto.ComputedRate;
import com.moola.fx.moneychanger.rate.strategy.RateCalculationStrategy;
import com.moola.fx.moneychanger.rate.dto.BasicRate;

import java.util.Map;
import java.util.List;

public class RateCalculationService {
    private final Map<String, RateCalculationStrategy> strategies;

    public RateCalculationService(Map<String, RateCalculationStrategy> strategies) {
        this.strategies = strategies;
    }

    public List<ComputedRate> processBatch (List<BasicRate> basicRates, String method) {
        RateCalculationStrategy strategy = strategies.get(method);
        if (strategy == null) {
            throw new IllegalArgumentException("Unknown method: " + method);
        }
        return strategy.compute(basicRates);
    }

}
