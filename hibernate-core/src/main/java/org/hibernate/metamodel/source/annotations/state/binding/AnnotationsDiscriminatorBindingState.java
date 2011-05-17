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
package org.hibernate.metamodel.source.annotations.state.binding;

import org.hibernate.metamodel.source.annotations.ColumnValues;
import org.hibernate.metamodel.source.annotations.DiscriminatorColumnValues;
import org.hibernate.metamodel.source.annotations.MappedAttribute;
import org.hibernate.metamodel.state.binding.DiscriminatorBindingState;

/**
 * @author Gail Badner
 *
 * TODO: extract a superclass that sets defaults for other stuff
 */
public class AnnotationsDiscriminatorBindingState
		extends AnnotationsAttributeBindingState implements DiscriminatorBindingState {
	private final boolean isForced;
	private final boolean isInserted;
	public AnnotationsDiscriminatorBindingState(MappedAttribute mappedAttribute) {
		super( mappedAttribute );
		DiscriminatorColumnValues columnValues = DiscriminatorColumnValues.class.cast( mappedAttribute.getColumnValues() );
		isForced = columnValues.isForced();
		isInserted = columnValues.isIncludedInSql();
	}

	@Override
	public boolean isForced() {
		return isForced;
	}

	@Override
	public boolean isInsertable() {
		return isInserted;
	}
}
