package life.genny.entity;


import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.DecimalFormat;

import javax.money.CurrencyUnit;
import javax.money.Monetary;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.javamoney.moneta.Money;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class MoneyDeserializer implements JsonSerializer<Money>, JsonDeserializer<Money> {

    protected static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());


    @Override
    public JsonElement serialize(Money src, Type typeOfSrc, JsonSerializationContext context) {
        JsonParser parser = new JsonParser();
        DecimalFormat decimalFormat = new DecimalFormat("###############0.00");

        String amount = decimalFormat.format(src.getNumber().doubleValue());
        if (amount.contains("+")) {
            log.debug("debug");
        }
        JsonElement o = parser
                .parse("{\"amount\":" + amount + ",\"currency\":\"" + src.getCurrency().getCurrencyCode() + "\"}");
        return o;
    }

    @Override
    public Money deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        final CurrencyUnit currency = Monetary.getCurrency(json.getAsJsonObject().get("currency").getAsString());

        if (StringUtils.isBlank(json.getAsJsonObject().get("amount").getAsString()))
            return null; // TODO, can we use Optional<Money> ?
        else {
            String amountStr = json.getAsJsonObject().get("amount").getAsString();
            if (amountStr.contains("+")) {
                log.debug("debug");
            }
            BigDecimal bDamount = new BigDecimal(amountStr);
            Money money = Money.of(bDamount, currency);

            return money;
        }
    }
}