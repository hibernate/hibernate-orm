/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
import org.hibernate.ejb.criteria.CriteriaBuilderImpl;
import org.hibernate.ejb.criteria.PathImpl;
import org.hibernate.ejb.criteria.CriteriaQueryCompiler;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class CollectionExpression<C> extends ExpressionImpl<C> {
	private final PathImpl origin;
	private final CollectionPersister persister;
	private final PluralAttribute<?, C, ?> attribute;

	public CollectionExpression(
			CriteriaBuilderImpl criteriaBuilder,
			Class<C> javaType,
			PathImpl origin,
			PluralAttribute<?, C, ?> attribute) {
		this( criteriaBuilder, javaType, resolvePersister( criteriaBuilder, attribute ), origin, attribute );
	}

	private static CollectionPersister resolvePersister(CriteriaBuilderImpl criteriaBuilder, PluralAttribute attribute) {
		SessionFactoryImplementor sfi = (SessionFactoryImplementor)
				criteriaBuilder.getEntityManagerFactory().getSessionFactory();
		return sfi.getCollectionPersister( resolveRole( attribute ) );
	}

	private static String resolveRole(PluralAttribute attribute) {
		return attribute.getDeclaringType().getJavaType().getName() +
				'.' + attribute.getName();
	}

	public CollectionExpression(
			CriteriaBuilderImpl criteriaBuilder,
			Class<C> javaType,
			CollectionPersister persister,
			PathImpl origin,
			PluralAttribute<?, C, ?> attribute) {
		super( criteriaBuilder, javaType );
		this.origin = origin;
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

	public String render(CriteriaQueryCompiler.RenderingContext renderingContext) {
		return origin.getPathIdentifier() + '.' + getAttribute().getName();
	}

	public String renderProjection(CriteriaQueryCompiler.RenderingContext renderingContext) {
		return render( renderingContext );
	}
}
