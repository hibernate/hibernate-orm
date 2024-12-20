/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
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
