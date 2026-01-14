/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.complete;

import org.hibernate.AssertionFailure;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.DiscriminatorMapping;
import org.hibernate.query.results.spi.FetchBuilder;
import org.hibernate.query.results.spi.FetchBuilderBasicValued;
import org.hibernate.query.results.MissingSqlSelectionException;
import org.hibernate.query.results.internal.DomainResultCreationStateImpl;
import org.hibernate.query.results.internal.ResultSetMappingSqlSelection;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.hibernate.query.results.internal.ResultsHelper.impl;
import static org.hibernate.query.results.internal.ResultsHelper.jdbcPositionToValuesArrayPosition;
import static org.hibernate.sql.ast.spi.SqlExpressionResolver.createColumnReferenceKey;

/**
 * @author Steve Ebersole
 */
public class CompleteFetchBuilderBasicPart implements CompleteFetchBuilder, FetchBuilderBasicValued, ModelPartReferenceBasic {
	private final NavigablePath navigablePath;
	private final BasicValuedModelPart referencedModelPart;
	private final String selectionAlias;

	public CompleteFetchBuilderBasicPart(
			NavigablePath navigablePath,
			BasicValuedModelPart referencedModelPart,
			String selectionAlias) {
		this.navigablePath = navigablePath;
		this.referencedModelPart = referencedModelPart;
		this.selectionAlias = selectionAlias;
	}

	@Override
	public FetchBuilder cacheKeyInstance() {
		return this;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public BasicValuedModelPart getReferencedPart() {
		return referencedModelPart;
	}

	@Override
	public List<String> getColumnAliases() {
		return Collections.singletonList( selectionAlias );
	}

	@Override
	public BasicFetch<?> buildFetch(
			FetchParent parent,
			NavigablePath fetchPath,
			JdbcValuesMetadata jdbcResultsMetadata,
			DomainResultCreationState domainResultCreationState) {
		final var creationStateImpl = impl( domainResultCreationState );

		final var tableReference =
				creationStateImpl.getFromClauseAccess()
						.getTableGroup( parent.getNavigablePath() )
						.resolveTableReference( navigablePath, referencedModelPart,
								referencedModelPart.getContainingTableExpression() );

		final int jdbcPosition = jdbcPosition( jdbcResultsMetadata, creationStateImpl );
		final String selectedAlias =
				selectionAlias == null
						? jdbcResultsMetadata.resolveColumnName( jdbcPosition )
						: selectionAlias;

		final var jdbcMapping =
				referencedModelPart instanceof DiscriminatorMapping discriminatorMapping
						? discriminatorMapping.getUnderlyingJdbcMapping()
						: referencedModelPart.getJdbcMapping();

		final int valuesArrayPosition = jdbcPositionToValuesArrayPosition( jdbcPosition );
		// we just care about the registration here.  The ModelPart will find it later
		creationStateImpl.resolveSqlExpression(
				createColumnReferenceKey( tableReference, referencedModelPart.getSelectablePath(), jdbcMapping ),
				processingState -> new ResultSetMappingSqlSelection( valuesArrayPosition, referencedModelPart )
		);

		return (BasicFetch<?>) parent.generateFetchableFetch(
				referencedModelPart,
				fetchPath,
				FetchTiming.IMMEDIATE,
				true,
				selectedAlias,
				domainResultCreationState
		);
	}

	private int jdbcPosition(JdbcValuesMetadata jdbcResultsMetadata, DomainResultCreationStateImpl creationStateImpl) {
		if ( selectionAlias != null ) {
			try {
				return jdbcResultsMetadata.resolveColumnPosition( selectionAlias );
			}
			catch (Exception e) {
				throw new MissingSqlSelectionException(
						"ResultSet mapping specified selected alias '" + selectionAlias
								+ "' which was not part of the ResultSet",
						e
				);
			}
		}
		else {
			if ( !creationStateImpl.arePositionalSelectionsAllowed() ) {
				throw new AssertionFailure( "Positional SQL selection resolution not allowed" );
			}
			return creationStateImpl.getNumberOfProcessedSelections() + 1;
		}
	}

	@Override
	public boolean equals(Object object) {
		if ( this == object ) {
			return true;
		}
		else if ( !( object instanceof CompleteFetchBuilderBasicPart that ) ) {
			return false;
		}
		else {
			return navigablePath.equals( that.navigablePath )
				&& referencedModelPart.equals( that.referencedModelPart )
				&& Objects.equals( selectionAlias, that.selectionAlias );
		}
	}

	@Override
	public int hashCode() {
		int result = navigablePath.hashCode();
		result = 31 * result + referencedModelPart.hashCode();
		result = 31 * result + ( selectionAlias != null ? selectionAlias.hashCode() : 0 );
		return result;
	}
}
