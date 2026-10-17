package com.examly.springapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.examly.springapp.model.enums.LabeledEnum;

/**
 * Lets query parameters use the same labels as JSON, e.g. ?status=Pending or ?category=Cement
 * (Spring's default conversion only accepts the exact constant name).
 *
 * Generic: ANY enum that implements LabeledEnum is handled automatically, so members add
 * their own enums without editing this file. Other enums keep Spring's normal behaviour.
 */
@Configuration
public class EnumConversionConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(new LabeledEnumConverterFactory());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static final class LabeledEnumConverterFactory implements ConverterFactory<String, Enum> {

        @Override
        public <T extends Enum> Converter<String, T> getConverter(Class<T> targetType) {
            return source -> {
                if (LabeledEnum.class.isAssignableFrom(targetType)) {
                    return (T) LabeledEnum.parse((Class) targetType, source);
                }
                return (T) Enum.valueOf(targetType, source.trim());
            };
        }
    }
}