/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.path;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.PluralJoin;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.Type;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.JoinImplementor;
import org.hibernate.query.criteria.internal.PathSource;

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

	@Override
	public JoinImplementor<O, E> on(Predicate... restrictions) {
		return super.on( restrictions );
	}

	@Override
	public JoinImplementor<O, E> on(Expression<Boolean> restriction) {
		return super.on( restriction );
	}
}
