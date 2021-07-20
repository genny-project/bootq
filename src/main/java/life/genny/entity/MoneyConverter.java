package life.genny.entity;


import java.lang.invoke.MethodHandles;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.apache.logging.log4j.Logger;
import org.javamoney.moneta.Money;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


@Converter
public class MoneyConverter implements AttributeConverter<Money, String> {

    private static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

    @Override
    public String convertToDatabaseColumn(final Money money) {
        String ret = "";
        GsonBuilder gsonBuilder = new GsonBuilder().registerTypeAdapter(Money.class, new MoneyDeserializer());
        Gson gson = gsonBuilder.create();

        ret = gson.toJson(money);


        return ret;

    }

    @Override
    public Money convertToEntityAttribute(String moneyStr) {
        GsonBuilder gsonBuilder = new GsonBuilder().registerTypeAdapter(Money.class, new MoneyDeserializer());
        Gson gson = gsonBuilder.create();
        Money money = gson.fromJson(moneyStr, Money.class);
        return money;
    }


}
