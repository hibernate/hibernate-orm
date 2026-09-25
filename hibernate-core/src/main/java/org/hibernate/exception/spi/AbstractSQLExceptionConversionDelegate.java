package org.hibernate.exception.spi;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSQLExceptionConversionDelegate implements SQLExceptionConversionDelegate {
	private final ConversionContext conversionContext;

	protected AbstractSQLExceptionConversionDelegate(ConversionContext conversionContext) {
		this.conversionContext = conversionContext;
	}

	protected ConversionContext getConversionContext() {
		return conversionContext;
	}
}
