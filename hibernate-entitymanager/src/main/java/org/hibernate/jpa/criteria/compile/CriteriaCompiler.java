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

import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.ParameterExpression;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		criteria.validate();

		final Map<ParameterExpression<?>,String> explicitParameterMapping = new HashMap<ParameterExpression<?>,String>();
		final Map<String,ParameterExpression<?>> explicitParameterNameMapping = new HashMap<String,ParameterExpression<?>>();
		final List<ImplicitParameterBinding> implicitParameterBindings = new ArrayList<ImplicitParameterBinding>();
		final Map<String,Class> implicitParameterTypes = new HashMap<String, Class>();

		RenderingContext renderingContext = new RenderingContext() {
			private int aliasCount = 0;
			private int explicitParameterCount = 0;

			public String generateAlias() {
				return "generatedAlias" + aliasCount++;
			}

			public String generateParameterName() {
				return "param" + explicitParameterCount++;
			}

			public String registerExplicitParameter(ParameterExpression<?> criteriaQueryParameter) {
				final String jpaqlParameterName;
				if ( explicitParameterMapping.containsKey( criteriaQueryParameter ) ) {
					jpaqlParameterName = explicitParameterMapping.get( criteriaQueryParameter );
				}
				else {
					jpaqlParameterName = generateParameterName();
					explicitParameterMapping.put( criteriaQueryParameter, jpaqlParameterName );
				}
				if ( StringHelper.isNotEmpty( criteriaQueryParameter.getName() ) ) {
					explicitParameterNameMapping.put(
							criteriaQueryParameter.getName(),
							criteriaQueryParameter
					);
				}
				return jpaqlParameterName;
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
				implicitParameterTypes.put( parameterName, javaType );
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
					public Map<ParameterExpression<?>, String> explicitParameterMapping() {
						return explicitParameterMapping;
					}

					@Override
					public Map<String, ParameterExpression<?>> explicitParameterNameMapping() {
						return explicitParameterNameMapping;
					}

					@Override
					public List<ImplicitParameterBinding> implicitParameterBindings() {
						return implicitParameterBindings;
					}

					@Override
					public Map<String, Class> implicitParameterTypes() {
						return implicitParameterTypes;
					}
				}
		);
	}

}
