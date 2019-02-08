/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.compile;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.ParameterExpression;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.query.criteria.LiteralHandlingMode;
import org.hibernate.query.criteria.internal.expression.function.FunctionExpression;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.sql.ast.Clause;
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
	private final SharedSessionContractImplementor entityManager;

	public CriteriaCompiler(SharedSessionContractImplementor entityManager) {
		this.entityManager = entityManager;
	}

	public QueryImplementor compile(CompilableCriteria criteria) {
		try {
			criteria.validate();
		}
		catch (IllegalStateException ise) {
			throw new IllegalArgumentException( "Error occurred validating the Criteria", ise );
		}

		final Map<ParameterExpression<?>, ExplicitParameterInfo<?>> explicitParameterInfoMap = new HashMap<>();
		final List<ImplicitParameterBinding> implicitParameterBindings = new ArrayList<>();

		final SessionFactoryImplementor sessionFactory = entityManager.getFactory();

		final LiteralHandlingMode criteriaLiteralHandlingMode = sessionFactory
				.getSessionFactoryOptions()
				.getCriteriaLiteralHandlingMode();

		final Dialect dialect = sessionFactory.getServiceRegistry().getService( JdbcServices.class ).getDialect();

		RenderingContext renderingContext = new RenderingContext() {
			private int aliasCount;
			private int explicitParameterCount;

			private final Stack<Clause> clauseStack = new StandardStack<>();
			private final Stack<FunctionExpression> functionContextStack = new StandardStack<>();

			public String generateAlias() {
				return "generatedAlias" + aliasCount++;
			}

			public String generateParameterName() {
				return "param" + explicitParameterCount++;
			}

			@Override
			public Stack<Clause> getClauseStack() {
				return clauseStack;
			}

			@Override
			public Stack<FunctionExpression> getFunctionStack() {
				return functionContextStack;
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
				SessionFactoryImplementor factory = entityManager.getFactory();
				Type hibernateType = factory.getTypeResolver().heuristicType( javaType.getName() );
				if ( hibernateType == null ) {
					throw new IllegalArgumentException(
							"Could not convert java type [" + javaType.getName() + "] to Hibernate type"
					);
				}
				return hibernateType.getName();
			}

			@Override
			public Dialect getDialect() {
				return dialect;
			}

			@Override
			public LiteralHandlingMode getCriteriaLiteralHandlingMode() {
				return criteriaLiteralHandlingMode;
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
