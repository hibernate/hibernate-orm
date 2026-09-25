package org.hibernate.type;

import jakarta.annotation.Nullable;

import org.hibernate.SPI;

import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/**
 * Extension for implementations of {@link BasicType} which have an implied
 * {@linkplain BasicValueConverter conversion}.
 */
@SPI({ USE, IMPLEMENT })
public interface ConvertedBasicType<J> extends BasicType<J> {
	@Nullable
	BasicValueConverter<J,?> getValueConverter();
}
