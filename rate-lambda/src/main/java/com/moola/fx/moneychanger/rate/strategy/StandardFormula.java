package com.moola.fx.moneychanger.rate.strategy;

import com.moola.fx.moneychanger.rate.dto.BasicRate;
import com.moola.fx.moneychanger.rate.dto.ComputedRate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.List;

public class StandardFormula implements RateCalculationStrategy {
    private static final int CALCULATION_SCALE_15 = 15;
    private static final int CALCULATION_SCALE_6 = 6;
    private static final int CALCULATION_SCALE_5 = 5;

    @Override
    public List<ComputedRate> compute(List<BasicRate> basicRates) {
        return basicRates.stream()
                .map(this::computeSingle)
                .toList();
    }

    private ComputedRate computeSingle(BasicRate basicRate) {
        ComputedRate result = new ComputedRate();

        result.setMoneyChangerId(basicRate.getMoneyChangerId());
        result.setCurrencyCode(basicRate.getCurrencyCode());
        result.setUnit(basicRate.getUnit());
        result.setTradeType(basicRate.getTradeType());
        result.setTradeDeno(basicRate.getTradeDeno());
        result.setTradeRound(basicRate.getTradeRound());
        result.setSpread(basicRate.getSpread());
        result.setSkew(basicRate.getSkew());
        result.setRefBid(basicRate.getRefBid());
        result.setRefAsk(basicRate.getRefAsk());
        result.setDpBid(basicRate.getDpBid());
        result.setDpAsk(basicRate.getDpAsk());
        result.setMarBid(basicRate.getMarBid());
        result.setMarAsk(basicRate.getMarAsk());
        result.setCfBid(basicRate.getCfBid());
        result.setCfAsk(basicRate.getCfAsk());
        result.setProcessedBy(basicRate.getProcessedBy());
        result.setProcessedAt(new Timestamp(System.currentTimeMillis()));

        result.setRawBid(basicRate.getRawBid().multiply(new BigDecimal(basicRate.getUnit())).setScale(CALCULATION_SCALE_6, RoundingMode.HALF_UP));
        result.setRawAsk(basicRate.getRawAsk().multiply(new BigDecimal(basicRate.getUnit())).setScale(CALCULATION_SCALE_6, RoundingMode.HALF_UP));

        // avg=(rawBid+rawAsk)/2
        BigDecimal avgRawBidAsk = (result.getRawBid()
                .add(result.getRawAsk()))
                .divide(BigDecimal.valueOf(2), CALCULATION_SCALE_15, RoundingMode.HALF_UP);

        // wsBid = avg + skew - (spread/2)
        BigDecimal wsBid= (avgRawBidAsk
                .add(basicRate.getSkew())
                .subtract((basicRate.getSpread().divide(BigDecimal.valueOf(2), CALCULATION_SCALE_15, RoundingMode.HALF_UP))))
                .setScale(CALCULATION_SCALE_5, RoundingMode.HALF_UP);
        result.setWsBid(wsBid);

        // wsAsk = avg + skew + (spread/2)
        BigDecimal wsAsk= (avgRawBidAsk
                .add(basicRate.getSkew())
                .add((basicRate.getSpread().divide(BigDecimal.valueOf(2), CALCULATION_SCALE_15, RoundingMode.HALF_UP))))
                .setScale(CALCULATION_SCALE_5, RoundingMode.HALF_UP);
        result.setWsAsk(wsAsk);


        //(1-marBid/100)*wsBid
        BigDecimal computedBid= (BigDecimal.ONE
                .subtract(basicRate.getMarBid().divide(BigDecimal.valueOf(100), CALCULATION_SCALE_15,RoundingMode.HALF_UP))
                .multiply(wsBid));

        if (BigDecimal.ZERO.equals(basicRate.getRefBid())) {
            // (1-marBid/100)*wsBid  --> round to dpBid  --> min (result, cfBid)
            BigDecimal rtBid = computedBid
                    .setScale(basicRate.getDpBid().intValue(),RoundingMode.HALF_UP)
                    .min(basicRate.getCfBid());
            result.setRtBid(rtBid);
        } else {
            // 1/ ((1-marBid/100)*wsBid)  --> round to dpBid  --> max(result, cfBid)
            BigDecimal rtBid = BigDecimal.ONE
                    .divide(computedBid, CALCULATION_SCALE_15, RoundingMode.HALF_UP)
                    .setScale(basicRate.getDpBid().intValue(),RoundingMode.HALF_UP)
                    .max(basicRate.getCfBid());
            result.setRtBid(rtBid);
        }

        // (1+marAsk/100)*wsAsk
        BigDecimal computedAsk= (BigDecimal.ONE
                .add(basicRate.getMarAsk().divide(BigDecimal.valueOf(100), CALCULATION_SCALE_15, RoundingMode.HALF_UP)))
                .multiply(wsAsk);

        if (BigDecimal.ZERO.equals(basicRate.getRefAsk())) {
            // (1+marAsk/100)*wsAsk  --> round to dpAsk  --> max (result, cfAsk)
            BigDecimal rtAsk = computedAsk
                    .setScale(basicRate.getDpAsk().intValue(),RoundingMode.HALF_UP)
                    .max(basicRate.getCfAsk());
            result.setRtAsk(rtAsk);
        } else {
            // 1/ ((1+marAsk/100)*wsAsk)  --> round to dpAsk  --> min (result, cfAsk)
            BigDecimal rtAsk = BigDecimal.ONE
                    .divide(computedAsk, CALCULATION_SCALE_15, RoundingMode.HALF_UP)
                    .setScale(basicRate.getDpAsk().intValue(), RoundingMode.HALF_UP)
                    .min(basicRate.getCfAsk());
            result.setRtAsk(rtAsk);
        }

        return result;
    }
}
