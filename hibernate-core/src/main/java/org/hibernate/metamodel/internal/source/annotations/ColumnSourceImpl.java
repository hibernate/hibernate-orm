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
package org.hibernate.metamodel.internal.source.annotations;

import org.hibernate.metamodel.internal.source.annotations.attribute.AttributeOverride;
import org.hibernate.metamodel.internal.source.annotations.attribute.BasicAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.Column;
import org.hibernate.metamodel.internal.source.annotations.attribute.MappedAttribute;

/**
 * @author Hardy Ferentschik
 */
public class ColumnSourceImpl extends ColumnValuesSourceImpl {
	private final MappedAttribute attribute;

	ColumnSourceImpl(MappedAttribute attribute, AttributeOverride attributeOverride, Column columnValues) {
		super( columnValues );
		if ( attributeOverride != null ) {
			setOverrideColumnValues( attributeOverride.getColumnValues() );
		}
		this.attribute = attribute;
	}

	@Override
	public String getReadFragment() {
		if ( attribute instanceof BasicAttribute ) {
			return ( ( BasicAttribute ) attribute ).getCustomReadFragment();
		}
		else {
			return null;
		}
	}

	@Override
	public String getWriteFragment() {
		if ( attribute instanceof BasicAttribute ) {
			return ( ( BasicAttribute ) attribute ).getCustomWriteFragment();
		}
		else {
			return null;
		}
	}

	@Override
	public String getCheckCondition() {
		if ( attribute instanceof BasicAttribute ) {
			return attribute.getCheckCondition();
		}
		else {
			return null;
		}
	}
}


