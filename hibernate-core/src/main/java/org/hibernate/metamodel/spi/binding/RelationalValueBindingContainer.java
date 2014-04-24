/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.spi.binding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.Value;

/**
 * @author Gail Badner
 */
public class RelationalValueBindingContainer {
	private final List<RelationalValueBinding> relationalValueBindings;
	private final boolean isListModifiable;

	public RelationalValueBindingContainer(List<RelationalValueBinding> relationalValueBindings) {
		this.relationalValueBindings = Collections.unmodifiableList( relationalValueBindings );
		this.isListModifiable = false;
	}

	public RelationalValueBindingContainer() {
		this.relationalValueBindings = new ArrayList<RelationalValueBinding>();
		this.isListModifiable = true;
	}

	public List<RelationalValueBinding> relationalValueBindings() {
		return isListModifiable
				? Collections.unmodifiableList( relationalValueBindings )
				: relationalValueBindings;
	}

	public List<Value> values() {
		final List<Value> values = new ArrayList<Value>( relationalValueBindings.size() );
		for ( RelationalValueBinding relationalValueBinding : relationalValueBindings ) {
			values.add( relationalValueBinding.getValue() );
		}
		return values;
	}

	public List<Column> columns() {
		final List<Column> columns = new ArrayList<Column>( relationalValueBindings.size() );
		for ( RelationalValueBinding relationalValueBinding : relationalValueBindings ) {
			if ( !relationalValueBinding.isDerived() ) {
				columns.add( (Column) relationalValueBinding.getValue() );
			}
		}
		return columns;
	}

	void addRelationalValueBindings(RelationalValueBindingContainer relationalValueBindingContainer) {
		if ( !isListModifiable ) {
			throw new IllegalStateException( "Cannot add relationalValueBindings because this object is unmodifiable." );
		}
		relationalValueBindings.addAll( relationalValueBindingContainer.relationalValueBindings );
	}

	public boolean hasDerivedValue() {
		for ( RelationalValueBinding relationalValueBinding : relationalValueBindings ) {
			if (relationalValueBinding.isDerived() ) {
				return true;
			}
		}
		return false;
	}

	public boolean hasNullableRelationalValueBinding() {
		for ( RelationalValueBinding relationalValueBinding : relationalValueBindings ) {
			if ( relationalValueBinding.isNullable() ) {
				return true;
			}
		}
		return false;
	}

	public boolean hasNonNullableRelationalValueBinding() {
		for ( RelationalValueBinding relationalValueBinding : relationalValueBindings ) {
			if ( !relationalValueBinding.isNullable() ) {
				return true;
			}
		}
		return false;
	}

	public boolean hasInsertableRelationalValueBinding() {
		for ( RelationalValueBinding relationalValueBinding : relationalValueBindings ) {
			if ( relationalValueBinding.isIncludeInInsert() ) {
				return true;
			}
		}
		return false;
	}

	public boolean hasUpdateableRelationalValueBinding() {
		for ( RelationalValueBinding relationalValueBinding : relationalValueBindings ) {
			if ( relationalValueBinding.isIncludeInUpdate() ) {
				return true;
			}
		}
		return false;
	}
}
