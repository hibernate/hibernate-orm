/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.spi;

import java.util.Objects;

import org.hibernate.models.spi.ClassDetails;

/**
 * @author Steve Ebersole
 */
public record ConverterRegistration(ClassDetails converterClass, Boolean autoApply) {
	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		ConverterRegistration that = (ConverterRegistration) o;
		return Objects.equals( converterClass, that.converterClass );
	}

	@Override
	public int hashCode() {
		return Objects.hash( converterClass );
	}
}
