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
package org.hibernate.metamodel.source.annotations.attribute;

import org.hibernate.internal.util.StringHelper;

/**
 * @author Hardy Ferentschik
 */
public class ColumnSourceImpl extends ColumnValuesSourceImpl {
	private final BasicAttribute attribute;
	private final String name;

	ColumnSourceImpl(BasicAttribute attribute, AttributeOverride attributeOverride) {
		super( attribute.getColumnValues() );
		if(attributeOverride != null) {
		   setOverrideColumnValues( attributeOverride.getColumnValues() );
		}
		this.attribute = attribute;
		this.name = resolveColumnName();
	}

	protected String resolveColumnName() {
		if ( StringHelper.isEmpty( super.getName() ) ) {
			//no @Column defined.
			return attribute.getContext().getNamingStrategy().propertyToColumnName( attribute.getName() );
		}
		else {
			return super.getName();
		}
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getReadFragment() {
		return attribute.getCustomReadFragment();
	}

	@Override
	public String getWriteFragment() {
		return attribute.getCustomWriteFragment();
	}

	@Override
	public String getCheckCondition() {
		return attribute.getCheckCondition();
	}
}


