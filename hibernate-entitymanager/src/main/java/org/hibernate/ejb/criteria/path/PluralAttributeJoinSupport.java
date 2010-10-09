/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.ejb.criteria.path;

import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.PluralJoin;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Bindable;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.Type;

import org.hibernate.ejb.criteria.CriteriaBuilderImpl;
import org.hibernate.ejb.criteria.PathSource;

/**
 * Support for defining joins to plural attributes (JPA requires typing based on
 * the specific collection type so we cannot really implement all support in a
 * single class)
 *
 * @author Steve Ebersole
 */
public abstract class PluralAttributeJoinSupport<O,C,E>
		extends AbstractJoinImpl<O,E>
		implements PluralJoin<O,C,E> {

	public PluralAttributeJoinSupport(
			CriteriaBuilderImpl criteriaBuilder,
			Class<E> javaType,
			PathSource<O> pathSource,
			Attribute<? super O,?> joinAttribute,
			JoinType joinType) {
		super( criteriaBuilder, javaType, pathSource, joinAttribute, joinType );
	}

	@Override
	public PluralAttribute<? super O, C, E> getAttribute() {
		return (PluralAttribute<? super O, C, E>) super.getAttribute();
	}

	public PluralAttribute<? super O, C, E> getModel() {
		return getAttribute();
	}

	@Override
	protected ManagedType<E> locateManagedType() {
		return isBasicCollection()
				? null
				: (ManagedType<E>) getAttribute().getElementType();
	}

	public boolean isBasicCollection() {
		return Type.PersistenceType.BASIC.equals( getAttribute().getElementType().getPersistenceType() );
	}

	@Override
	protected boolean canBeDereferenced() {
		return !isBasicCollection();
	}

	@Override
	protected boolean canBeJoinSource() {
		return !isBasicCollection();
	}
}
