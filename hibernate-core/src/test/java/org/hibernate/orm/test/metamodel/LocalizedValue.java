/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.metamodel;

import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class LocalizedValue implements ILocalizable {

	@Column(name = "val")
	private String value;

	@Override
	public String getValue() {
		return value;
	}

	@Override
	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( ( o == null ) || ( getClass() != o.getClass() ) ) {
			return false;
		}
		LocalizedValue that = ( (LocalizedValue) o );
		return Objects.equals( value, that.value );
	}

	@Override
	public int hashCode() {
		return Objects.hash( value );
	}
}
