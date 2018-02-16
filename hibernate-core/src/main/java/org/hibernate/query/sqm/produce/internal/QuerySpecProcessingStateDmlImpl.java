/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.internal;

import java.util.Collections;
import java.util.List;

import org.hibernate.metamodel.model.domain.spi.EmbeddedValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.query.sqm.ParsingException;
import org.hibernate.query.sqm.produce.SqmProductionException;
import org.hibernate.query.sqm.produce.spi.AbstractQuerySpecProcessingState;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.produce.spi.SqmFromBuilder;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmFromElementSpace;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.query.sqm.tree.from.SqmNavigableJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;

/**
 * QuerySpecProcessingState implementation for DML statements
 *
 * @author Steve Ebersole
 */
public class QuerySpecProcessingStateDmlImpl extends AbstractQuerySpecProcessingState {
	private final DmlFromClause fromClause;

	private final SqmFromBuilder fromElementBuilder;

	public QuerySpecProcessingStateDmlImpl(SqmCreationContext creationContext) {
		// implicitly no outer query, so pass null
		super( creationContext, null );
		this.fromClause = new DmlFromClause();
		this.fromElementBuilder = new DmlFromElementBuilder( creationContext );
	}

	@Override
	public DmlFromClause getFromClause() {
		return fromClause;
	}

	@Override
	public SqmNavigableReference findNavigableReferenceByIdentificationVariable(String identificationVariable) {
		return fromClause.fromElementSpace.getRoot().getIdentificationVariable().equals( identificationVariable )
				? fromClause.fromElementSpace.getRoot().getNavigableReference()
				: null;
	}

	@Override
	public SqmNavigableReference findNavigableReferenceExposingNavigable(String navigableName) {
		if ( rootExposesAttribute( navigableName ) ) {
			return fromClause.fromElementSpace.getRoot().getNavigableReference();
		}
		else {
			return null;
		}
	}

	private boolean rootExposesAttribute(String attributeName) {
		return null != fromClause.fromElementSpace.getRoot().getNavigableReference().getReferencedNavigable().findNavigable( attributeName );
	}

	public DmlSqmFromElementSpace getDmlFromElementSpace() {
		return fromClause.fromElementSpace;
	}

	public static class DmlFromClause extends SqmFromClause {
		private final DmlSqmFromElementSpace fromElementSpace = new DmlSqmFromElementSpace( this );

		@Override
		public List<SqmFromElementSpace> getFromElementSpaces() {
			return Collections.singletonList( fromElementSpace );
		}

		@Override
		public void addFromElementSpace(SqmFromElementSpace space) {
			throw new ParsingException( "DML from-clause cannot have additional FromElementSpaces" );
		}

		@Override
		public SqmFromElementSpace makeFromElementSpace() {
			throw new ParsingException( "DML from-clause cannot have additional FromElementSpaces" );
		}
	}

	public static class DmlSqmFromElementSpace extends SqmFromElementSpace {
		private DmlSqmFromElementSpace(DmlFromClause fromClause) {
			super( fromClause );
		}

		@Override
		public void setRoot(SqmRoot root) {
			super.setRoot( root );
		}

		@Override
		public List<SqmJoin> getJoins() {
			return Collections.emptyList();
		}

		@Override
		public void addJoin(SqmJoin join) {
			throw new ParsingException( "DML from-clause cannot define joins" );
		}
	}

	public static class DmlFromElementBuilder extends SqmFromBuilderStandard {
		public DmlFromElementBuilder(SqmCreationContext creationContext) {
			super( creationContext );
		}

		@Override
		public SqmCrossJoin buildCrossJoin(EntityValuedNavigable navigable) {
			throw new SqmProductionException( "DML from-clause cannot define joins" );
		}

		@Override
		public SqmEntityJoin buildEntityJoin(EntityValuedNavigable navigable) {
			throw new SqmProductionException( "DML from-clause cannot define joins" );
		}

		@Override
		public SqmNavigableJoin buildNavigableJoin(SqmNavigableReference navigableReference) {
			if ( EmbeddedValuedNavigable.class.isInstance( navigableReference.getReferencedNavigable() ) ) {
				return super.buildNavigableJoin( navigableReference );
			}

			throw new SqmProductionException( "DML from-clause cannot define joins" );
		}
	}
}
