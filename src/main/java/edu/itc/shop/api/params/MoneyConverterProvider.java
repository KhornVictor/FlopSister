package edu.itc.shop.api.params;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import edu.itc.shop.domain.Money;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

@Provider
public class MoneyConverterProvider implements ParamConverterProvider {

    @Override
    @SuppressWarnings("unchecked")
    public <T> ParamConverter<T> getConverter(
            Class<T> rawType,
            Type genericType,
            Annotation[] annotations) {
        if (rawType != Money.class) {
            return null;
        }

        ParamConverter<Money> converter = new ParamConverter<>() {
            @Override
            public Money fromString(String value) {
                if (value == null) {
                    return null;
                }
                try {
                    return Money.parse(value);
                } catch (IllegalArgumentException exception) {
                    throw new BadRequestException(exception.getMessage(), exception);
                }
            }

            @Override
            public String toString(Money value) {
                return value.toString();
            }
        };
        return (ParamConverter<T>) converter;
    }
}
