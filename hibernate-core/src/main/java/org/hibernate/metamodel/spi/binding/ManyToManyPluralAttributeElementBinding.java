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
package org.hibernate.metamodel.spi.binding;

import java.util.HashMap;

import org.hibernate.metamodel.spi.relational.Table;
import org.hibernate.metamodel.spi.relational.Value;

/**
 * Describes plural attributes of {@link PluralAttributeElementNature#MANY_TO_MANY} elements
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class ManyToManyPluralAttributeElementBinding extends AbstractPluralAttributeAssociationElementBinding {
	private final java.util.Map manyToManyFilters = new HashMap();
	private String manyToManyWhere;
	private String manyToManyOrderBy;
	// TODO: really should have value defined (which defines table), but may not know 
	private Value value;

	ManyToManyPluralAttributeElementBinding(AbstractPluralAttributeBinding binding) {
		super( binding );
	}

	@Override
	public PluralAttributeElementNature getPluralAttributeElementNature() {
		return PluralAttributeElementNature.MANY_TO_MANY;
	}

	public String getManyToManyWhere() {
		return manyToManyWhere;
	}

	public void setManyToManyWhere(String manyToManyWhere) {
		this.manyToManyWhere = manyToManyWhere;
	}

	public String getManyToManyOrderBy() {
		return manyToManyOrderBy;
	}

	public void setManyToManyOrderBy(String manyToManyOrderBy) {
		this.manyToManyOrderBy = manyToManyOrderBy;
	}

	public Value getValue() {
		return value;
	}

	public void setValue(Value value) {
		this.value = value;
	}
}
