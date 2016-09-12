/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.Query;
import org.hibernate.query.QueryParameter;
import org.hibernate.sqm.query.Parameter;
import org.hibernate.sqm.query.SqmStatement;
import org.hibernate.sqm.query.SqmStatementNonSelect;

/**
 * {@link Query} implementation based on an SQM
 *
 * @author Steve Ebersole
 */
public class QuerySqmImpl<R> extends AbstractProducedQuery<R> implements Query<R> {
	private final String sourceQueryString;
	private final SqmStatement sqmStatement;
	private final Class resultType;

	public QuerySqmImpl(
			String sourceQueryString,
			SqmStatement sqmStatement,
			Class resultType,
			SharedSessionContractImplementor producer) {
		super( producer, extractParameterMetadata( sqmStatement ) );
		this.sourceQueryString = sourceQueryString;
		this.sqmStatement = sqmStatement;
		this.resultType = resultType;

		if ( resultType != null ) {
			if ( sqmStatement instanceof SqmStatementNonSelect ) {
				throw new IllegalArgumentException( "Non-select queries cannot be typed" );
			}
		}
	}

	private static ParameterMetadata extractParameterMetadata(SqmStatement sqm) {
		Map<String, QueryParameter> namedQueryParameters = null;
		Map<Integer, QueryParameter> positionalQueryParameters = null;

		for ( Parameter parameter : sqm.getQueryParameters() ) {
			if ( parameter.getName() != null ) {
				if ( namedQueryParameters == null ) {
					namedQueryParameters = new HashMap<>();
				}
				namedQueryParameters.put(
						parameter.getName(),
						new NamedQueryParameterStandardImpl(
								parameter.getName(),
								parameter.allowMultiValuedBinding(),
								parameter.getAnticipatedType()
						)
				);
			}
			else if ( parameter.getPosition() != null ) {
				if ( positionalQueryParameters == null ) {
					positionalQueryParameters = new HashMap<>();
				}
				positionalQueryParameters.put(
						parameter.getPosition(),
						new PositionalQueryParameterStandardImpl(
								parameter.getPosition(),
								parameter.allowMultiValuedBinding(),
								parameter.getAnticipatedType()
						)
				);
			}
		}

		return new ParameterMetadataImpl( namedQueryParameters, positionalQueryParameters );
	}

	@Override
	public String getQueryString() {
		return sourceQueryString;
	}

	@Override
	protected boolean isNativeQuery() {
		return false;
	}

	public SqmStatement getSqmStatement() {
		return sqmStatement;
	}

	public Class getResultType() {
		return resultType;
	}
}
