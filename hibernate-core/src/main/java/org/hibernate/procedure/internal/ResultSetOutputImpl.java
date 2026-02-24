/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure.internal;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.hibernate.TypeMismatchException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.procedure.ResultSetOutput;
import org.hibernate.query.results.internal.jpa.JpaMappingHelper;
import org.hibernate.query.results.spi.ResultSetMapping;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Implementation of ResultSetOutput
 *
 * @author Steve Ebersole
 */
public class ResultSetOutputImpl<T> implements ResultSetOutput<T> {
	private final Function<ResultSetMapping,List<T>> resultSetSupplier;
	private final SessionFactoryImplementor sessionFactory;

	private ResultSetMapping resultSetMapping;
	private List<T> resultList;

	public ResultSetOutputImpl(
			@NonNull Function<ResultSetMapping,List<T>> resultSetSupplier,
			ResultSetMapping declaredMapping,
			SessionFactoryImplementor sessionFactory) {
		this.resultSetSupplier = resultSetSupplier;
		this.resultSetMapping = declaredMapping;
		this.sessionFactory = sessionFactory;
	}

	@Override
	public boolean isResultSet() {
		return true;
	}

	@Override
	public <X> ResultSetOutput<X> asResultSetOutput(Class<X> resultType) {
		if ( resultSetMapping == null
				|| (resultSetMapping.isDynamic() && resultSetMapping.getNumberOfResultBuilders() == 0) ) {
			resultSetMapping = Util.makeResultSetMapping(
					null,
					resultType,
					null,
					() -> sessionFactory
			);
		}
		else if ( resultSetMapping.getNumberOfResultBuilders() == 1 ) {
			var resultBuilder = resultSetMapping.getResultBuilders().get( 0 );
			var outputJavaType = resultBuilder.getJavaType();
			if ( outputJavaType != null && !resultType.isAssignableFrom( outputJavaType ) ) {
				throw new TypeMismatchException( String.format( Locale.ROOT,
						"Output type [%s] cannot be assigned to requested type [%s]",
						outputJavaType.getName(),
						resultType.getName()
				) );
			}
		}

		//noinspection unchecked
		return (ResultSetOutput<X>) this;
	}

	@Override
	public <X> ResultSetOutput<X> asResultSetOutput(jakarta.persistence.sql.ResultSetMapping<X> japMMapping) {
		this.resultSetMapping = JpaMappingHelper.toHibernateMapping( japMMapping, sessionFactory );
		//noinspection unchecked
		return (ResultSetOutput<X>) this;
	}

	@Override
	public List<T> getResultList() {
		if ( resultList == null ) {
			resultList = resultSetSupplier.apply( resultSetMapping );
		}
		return resultList;
	}

	@Override
	public Object getSingleResult() {
		final List<?> results = getResultList();
		return results == null || results.isEmpty() ? null : results.get( 0 );
	}
}
