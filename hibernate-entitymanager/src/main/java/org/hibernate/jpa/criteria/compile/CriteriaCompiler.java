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
package org.hibernate.jpa.criteria.compile;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.ParameterExpression;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jpa.spi.HibernateEntityManagerImplementor;
import org.hibernate.type.Type;

/**
 * Compiles a JPA criteria query into an executable {@link TypedQuery}.  Its single contract is the {@link #compile}
 * method.
 * <p/>
 * NOTE : This is a temporary implementation which simply translates the criteria query into a JPAQL query string.  A
 * better, long-term solution is being implemented as part of refactoring the JPAQL/HQL translator.
 *
 * @author Steve Ebersole
 */
public class CriteriaCompiler implements Serializable {
	private final HibernateEntityManagerImplementor entityManager;

	public CriteriaCompiler(HibernateEntityManagerImplementor entityManager) {
		this.entityManager = entityManager;
	}

	public Query compile(CompilableCriteria criteria) {
		try {
			criteria.validate();
		}
		catch (IllegalStateException ise) {
			throw new IllegalArgumentException( "Error occurred validating the Criteria", ise );
		}

		final Map<ParameterExpression<?>, ExplicitParameterInfo<?>> explicitParameterInfoMap =
				new HashMap<ParameterExpression<?>, ExplicitParameterInfo<?>>();

		final List<ImplicitParameterBinding> implicitParameterBindings = new ArrayList<ImplicitParameterBinding>();

		RenderingContext renderingContext = new RenderingContext() {
			private int aliasCount;
			private int explicitParameterCount;

			public String generateAlias() {
				return "generatedAlias" + aliasCount++;
			}

			public String generateParameterName() {
				return "param" + explicitParameterCount++;
			}

			@Override
			@SuppressWarnings("unchecked")
			public ExplicitParameterInfo registerExplicitParameter(ParameterExpression<?> criteriaQueryParameter) {
				ExplicitParameterInfo parameterInfo = explicitParameterInfoMap.get( criteriaQueryParameter );
				if ( parameterInfo == null ) {
					if ( StringHelper.isNotEmpty( criteriaQueryParameter.getName() ) ) {
						parameterInfo = new ExplicitParameterInfo(
								criteriaQueryParameter.getName(),
								null,
								criteriaQueryParameter.getJavaType()
						);
					}
					else if ( criteriaQueryParameter.getPosition() != null ) {
						parameterInfo = new ExplicitParameterInfo(
								null,
								criteriaQueryParameter.getPosition(),
								criteriaQueryParameter.getJavaType()
						);
					}
					else {
						parameterInfo = new ExplicitParameterInfo(
								generateParameterName(),
								null,
								criteriaQueryParameter.getJavaType()
						);
					}

					explicitParameterInfoMap.put( criteriaQueryParameter, parameterInfo );
				}

				return parameterInfo;
			}

			public String registerLiteralParameterBinding(final Object literal, final Class javaType) {
				final String parameterName = generateParameterName();
				final ImplicitParameterBinding binding = new ImplicitParameterBinding() {
					public String getParameterName() {
						return parameterName;
					}

					public Class getJavaType() {
						return javaType;
					}

					public void bind(TypedQuery typedQuery) {
						typedQuery.setParameter( parameterName, literal );
					}
				};

				implicitParameterBindings.add( binding );
				return parameterName;
			}

			public String getCastType(Class javaType) {
				SessionFactoryImplementor factory =
						( SessionFactoryImplementor ) entityManager.getFactory().getSessionFactory();
				Type hibernateType = factory.getTypeResolver().heuristicType( javaType.getName() );
				if ( hibernateType == null ) {
					throw new IllegalArgumentException(
							"Could not convert java type [" + javaType.getName() + "] to Hibernate type"
					);
				}
				return hibernateType.getName();
			}
		};

		return criteria.interpret( renderingContext ).buildCompiledQuery(
				entityManager,
				new InterpretedParameterMetadata() {
					@Override
					public Map<ParameterExpression<?>, ExplicitParameterInfo<?>> explicitParameterInfoMap() {
						return explicitParameterInfoMap;
					}

					@Override
					public List<ImplicitParameterBinding> implicitParameterBindings() {
						return implicitParameterBindings;
					}
				}
		);
	}

}
