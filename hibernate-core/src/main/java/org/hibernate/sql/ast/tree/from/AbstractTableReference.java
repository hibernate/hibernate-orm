/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.from;

import java.util.Objects;

public abstract class AbstractTableReference implements TableReference {
	protected final String identificationVariable;
	protected final boolean isOptional;

	public AbstractTableReference(String identificationVariable, boolean isOptional) {
		assert identificationVariable != null;
		this.identificationVariable = identificationVariable;
		this.isOptional = isOptional;
	}

	@Override
	public String getIdentificationVariable() {
		return identificationVariable;
	}

	@Override
	public boolean isOptional() {
		return isOptional;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		TableReference that = (TableReference) o;
		return Objects.equals( identificationVariable, that.getIdentificationVariable() );
	}

	@Override
	public int hashCode() {
		return Objects.hash( identificationVariable );
	}
}
