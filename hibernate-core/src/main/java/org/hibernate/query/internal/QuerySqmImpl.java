/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.TransactionRequiredException;

import org.hibernate.LockMode;
import org.hibernate.ScrollMode;
import org.hibernate.engine.query.spi.EntityGraphQueryHint;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.jpa.graph.internal.EntityGraphImpl;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.Query;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.spi.MutableQueryOptions;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.spi.SqmBackedQuery;
import org.hibernate.sql.sqm.exec.internal.QueryOptionsImpl;
import org.hibernate.sqm.query.Parameter;
import org.hibernate.sqm.query.SqmStatement;
import org.hibernate.sqm.query.SqmStatementNonSelect;

/**
 * {@link Query} implementation based on an SQM
 *
 * @author Steve Ebersole
 */
public class QuerySqmImpl<R> extends AbstractProducedQuery<R> implements SqmBackedQuery<R> {
	private final String sourceQueryString;
	private final SqmStatement sqmStatement;
	private final Class resultType;

	private final QueryOptionsImpl queryOptions = new QueryOptionsImpl();

	private EntityGraphQueryHint entityGraphQueryHint;

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

	public SqmStatement getSqmStatement() {
		return sqmStatement;
	}

	public Class<R> getResultType() {
		return resultType;
	}

	@Override
	public MutableQueryOptions getQueryOptions() {
		return queryOptions;
	}

	@Override
	public EntityGraphQueryHint getEntityGraphHint() {
		return entityGraphQueryHint;
	}

	@Override
	protected boolean isNativeQuery() {
		return false;
	}

	@Override
	protected void applyEntityGraphQueryHint(String hintName, EntityGraphImpl entityGraph) {
		this.entityGraphQueryHint = new EntityGraphQueryHint( hintName, entityGraph );
	}


	@Override
	protected void collectHints(Map<String, Object> hints) {
		super.collectHints( hints );

		if ( entityGraphQueryHint != null ) {
			hints.put( entityGraphQueryHint.getHintName(), entityGraphQueryHint.getOriginEntityGraph() );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	protected List<R> doList() {
		verifyTransactionInProgressIfRequired();

		return getProducer().performList( this );

//		return getProducer().list(
//				sqmStatement,
//				resultType,
//				entityGraphQueryHint,
//				getQueryOptions(),
//				getQueryParameterBindings()
//		);
	}

	private void verifyTransactionInProgressIfRequired() {
		final LockMode lockMode = getLockOptions().findGreatestLockMode();
		if ( lockMode != null && lockMode.greaterThan( LockMode.NONE ) ) {
			if ( !getProducer().isTransactionInProgress() ) {
				throw new TransactionRequiredException(
						"Locking [" + lockMode.name() + "] was requested on Query, but no transaction is in progress"
				);
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Iterator<R> doIterate() {
		verifyTransactionInProgressIfRequired();

		return getProducer().performIterate( this );
//		return getProducer().iterate(
//				sqmStatement,
//				resultType,
//				entityGraphQueryHint,
//				getQueryOptions(),
//				getQueryParameterBindings()
//		);
	}

	@Override
	protected ScrollableResultsImplementor doScroll(ScrollMode scrollMode) {
		verifyTransactionInProgressIfRequired();

		return getProducer().performScroll( this, scrollMode );
//		return getProducer().scroll(
//				sqmStatement,
//				resultType,
//				entityGraphQueryHint,
//				getQueryOptions(),
//				getQueryParameterBindings()
//		);
	}

	@Override
	protected int doExecuteUpdate() {
		return getProducer().executeUpdate( this );
//		return getProducer().executeUpdate(
//				sqmStatement,
//				resultType,
//				entityGraphQueryHint,
//				getQueryOptions(),
//				getQueryParameterBindings()
//		);
	}
}
