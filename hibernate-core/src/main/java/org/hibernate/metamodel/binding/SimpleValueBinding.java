/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.binding;

import org.hibernate.metamodel.relational.Column;
import org.hibernate.metamodel.relational.DerivedValue;
import org.hibernate.metamodel.relational.SimpleValue;

/**
 * @author Steve Ebersole
 */
public class SimpleValueBinding {
	private SimpleValue simpleValue;
	private boolean includeInInsert;
	private boolean includeInUpdate;

	public SimpleValueBinding() {
		this( true, true );
	}

	public SimpleValueBinding(SimpleValue simpleValue) {
		this();
		setSimpleValue( simpleValue );
	}

	public SimpleValueBinding(SimpleValue simpleValue, boolean includeInInsert, boolean includeInUpdate) {
		this( includeInInsert, includeInUpdate );
		setSimpleValue( simpleValue );
	}

	public SimpleValueBinding(boolean includeInInsert, boolean includeInUpdate) {
		this.includeInInsert = includeInInsert;
		this.includeInUpdate = includeInUpdate;
	}

	public SimpleValue getSimpleValue() {
		return simpleValue;
	}

	public void setSimpleValue(SimpleValue simpleValue) {
		this.simpleValue = simpleValue;
		if ( DerivedValue.class.isInstance( simpleValue ) ) {
			includeInInsert = false;
			includeInUpdate = false;
		}
	}

	public boolean isDerived() {
		return DerivedValue.class.isInstance( simpleValue );
	}

	public boolean isNullable() {
		return isDerived() || Column.class.cast( simpleValue ).isNullable();
	}

	/**
	 * Is the value to be inserted as part of its binding here?
	 * <p/>
	 * <b>NOTE</b> that a column may be bound to multiple attributes.  The purpose of this value is to track this
	 * notion of "insertability" for this particular binding.
	 *
	 * @return {@code true} indicates the value should be included; {@code false} indicates it should not
	 */
	public boolean isIncludeInInsert() {
		return includeInInsert;
	}

	public void setIncludeInInsert(boolean includeInInsert) {
		this.includeInInsert = includeInInsert;
	}

	public boolean isIncludeInUpdate() {
		return includeInUpdate;
	}

	public void setIncludeInUpdate(boolean includeInUpdate) {
		this.includeInUpdate = includeInUpdate;
	}
}
