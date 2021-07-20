package life.genny.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import life.genny.entity.MoneyDeserializer;
import org.javamoney.moneta.Money;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class JsonUtils {
    static GsonBuilder gsonBuilder = new GsonBuilder();
    static public Gson gson = gsonBuilder.registerTypeAdapter(Money.class, new MoneyDeserializer())
            .registerTypeAdapter(LocalDateTime.class, new DateTimeDeserializer())
            .registerTypeAdapter(LocalDate.class, new LocalDateConverter())
            //	.excludeFieldsWithModifiers(Modifier.STATIC, Modifier.TRANSIENT, Modifier.VOLATILE)
            .excludeFieldsWithoutExposeAnnotation()
            //    .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    public static String toJson(Object obj) {

        String ret = gson.toJson(obj);
        return ret;
    }
}
