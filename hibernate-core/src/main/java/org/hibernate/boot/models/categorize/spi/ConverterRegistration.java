/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.categorize.spi;

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
