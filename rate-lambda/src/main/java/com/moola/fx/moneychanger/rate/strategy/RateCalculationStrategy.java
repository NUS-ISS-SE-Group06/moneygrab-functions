package com.moola.fx.moneychanger.rate.strategy;

import com.moola.fx.moneychanger.rate.dto.BasicRate;
import com.moola.fx.moneychanger.rate.dto.ComputedRate;

import java.util.List;

public interface RateCalculationStrategy {
    List<ComputedRate> compute(List<BasicRate> basicRates);
}