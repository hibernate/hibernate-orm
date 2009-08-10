/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.ejb.criteria.expression;

import javax.persistence.metamodel.PluralAttribute;
import org.hibernate.ejb.criteria.ParameterRegistry;
import org.hibernate.ejb.criteria.QueryBuilderImpl;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class CollectionExpression<C> extends ExpressionImpl<C> {
	private final CollectionPersister persister;
	private final PluralAttribute<?, C, ?> attribute;

	public CollectionExpression(
			QueryBuilderImpl queryBuilder,
			Class<C> javaType,
			PluralAttribute<?, C, ?> attribute) {
		this( queryBuilder, javaType, resolvePersister( queryBuilder, attribute ), attribute );
	}

	private static CollectionPersister resolvePersister(QueryBuilderImpl queryBuilder, PluralAttribute attribute) {
		SessionFactoryImplementor sfi = (SessionFactoryImplementor)
				queryBuilder.getEntityManagerFactory().getSessionFactory();
		return sfi.getCollectionPersister( resolveRole( attribute ) );
	}

	private static String resolveRole(PluralAttribute attribute) {
		return attribute.getDeclaringType().getJavaType().getName() +
				'.' + attribute.getName();
	}

	public CollectionExpression(
			QueryBuilderImpl queryBuilder,
			Class<C> javaType,
			CollectionPersister persister,
			PluralAttribute<?, C, ?> attribute) {
		super(queryBuilder, javaType);
		this.persister = persister;
		this.attribute = attribute;
	}

	public PluralAttribute<?, C, ?> getAttribute() {
		return attribute;
	}

	public CollectionPersister getPersister() {
		return persister;
	}

	public void registerParameters(ParameterRegistry registry) {
		// none to register
	}
}
