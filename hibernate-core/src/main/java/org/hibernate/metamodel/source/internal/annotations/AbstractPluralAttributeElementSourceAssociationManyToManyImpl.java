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
package org.hibernate.metamodel.source.internal.annotations;

import org.hibernate.engine.FetchTiming;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.source.internal.annotations.attribute.AbstractPersistentAttribute;
import org.hibernate.metamodel.source.spi.FilterSource;
import org.hibernate.metamodel.source.spi.PluralAttributeElementSourceManyToMany;
import org.hibernate.metamodel.spi.PluralAttributeElementNature;

/**
 * @author Gail Badner
 */
public abstract class AbstractPluralAttributeElementSourceAssociationManyToManyImpl
		extends AbstractPluralElementSourceAssociationImpl
		implements PluralAttributeElementSourceManyToMany {


	public AbstractPluralAttributeElementSourceAssociationManyToManyImpl(PluralAttributeSourceImpl pluralAttributeSource) {
		super( pluralAttributeSource );
	}

	@Override
	public boolean isUnique() {
		return pluralAssociationAttribute().getNature() == AbstractPersistentAttribute.Nature.ONE_TO_MANY;
	}

	@Override
	public String getReferencedEntityAttributeName() {
		// HBM only
		return null;
	}

	@Override
	public FilterSource[] getFilterSources() {
		return new FilterSource[0];  //todo
	}

	@Override
	public String getWhere() {
		return pluralAssociationAttribute().getWhereClause();
	}

	@Override
	public FetchTiming getFetchTiming() {
		return FetchTiming.IMMEDIATE;
	}

	@Override
	public PluralAttributeElementNature getNature() {
		return PluralAttributeElementNature.MANY_TO_MANY;
	}

	@Override
	public boolean isOrdered() {
		return StringHelper.isNotEmpty( pluralAssociationAttribute().getOrderBy() );
	}

	@Override
	public String getOrder() {
		return pluralAssociationAttribute().getOrderBy();
	}
}
