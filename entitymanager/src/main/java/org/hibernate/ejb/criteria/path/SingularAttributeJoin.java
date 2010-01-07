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
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Bindable;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.ejb.criteria.CriteriaBuilderImpl;
import org.hibernate.ejb.criteria.CriteriaSubqueryImpl;
import org.hibernate.ejb.criteria.FromImplementor;
import org.hibernate.ejb.criteria.PathSource;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class SingularAttributeJoin<Z,X> extends AbstractJoinImpl<Z,X> {
	private final Bindable<X> model;

	@SuppressWarnings({ "unchecked" })
	public SingularAttributeJoin(
			CriteriaBuilderImpl criteriaBuilder,
			Class<X> javaType,
			PathSource<Z> pathSource, 
			SingularAttribute<? super Z, ?> joinAttribute,
			JoinType joinType) {
		super( criteriaBuilder, javaType, pathSource, joinAttribute, joinType );
		this.model = (Bindable<X>) (
				Attribute.PersistentAttributeType.EMBEDDED == joinAttribute.getPersistentAttributeType()
						? joinAttribute
						: criteriaBuilder.getEntityManagerFactory().getMetamodel().managedType( javaType )
		);
	}

	@Override
	public SingularAttribute<? super Z, ?> getAttribute() {
		return (SingularAttribute<? super Z, ?>) super.getAttribute();
	}

	@Override
	public SingularAttributeJoin<Z, X> correlateTo(CriteriaSubqueryImpl subquery) {
		return (SingularAttributeJoin<Z, X>) super.correlateTo( subquery );
	}

	@Override
	protected FromImplementor<Z, X> createCorrelationDelegate() {
		return new SingularAttributeJoin<Z,X>(
				criteriaBuilder(),
				getJavaType(),
				getPathSource(),
				getAttribute(),
				getJoinType()
		);
	}

	@Override
	protected boolean canBeJoinSource() {
		return true;
	}

	public Bindable<X> getModel() {
		return model;
	}
}
