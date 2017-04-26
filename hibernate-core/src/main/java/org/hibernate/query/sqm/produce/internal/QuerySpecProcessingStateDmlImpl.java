/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.internal;

import java.util.Collections;
import java.util.List;

import org.hibernate.query.sqm.produce.spi.AliasRegistry;
import org.hibernate.query.sqm.produce.spi.FromElementLocator;
import org.hibernate.query.sqm.produce.spi.ParsingContext;
import org.hibernate.query.sqm.domain.SqmExpressableTypeEmbedded;
import org.hibernate.query.sqm.domain.SqmExpressableTypeEntity;
import org.hibernate.query.sqm.ParsingException;
import org.hibernate.query.sqm.produce.spi.AbstractQuerySpecProcessingState;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.expression.domain.SqmAttributeReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.from.SqmFromElementSpace;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;

/**
 * QuerySpecProcessingState implementation for DML statements
 *
 * @author Steve Ebersole
 */
public class QuerySpecProcessingStateDmlImpl extends AbstractQuerySpecProcessingState {
	private final DmlFromClause fromClause;

	private final FromElementBuilder fromElementBuilder;

	public QuerySpecProcessingStateDmlImpl(ParsingContext parsingContext) {
		// implicitly no outer query, so pass null
		super( parsingContext, null );
		this.fromClause = new DmlFromClause();
		this.fromElementBuilder = new DmlFromElementBuilder( parsingContext, new AliasRegistry() );
	}

	@Override
	public DmlFromClause getFromClause() {
		return fromClause;
	}

	@Override
	public SqmNavigableReference findNavigableBindingByIdentificationVariable(String identificationVariable) {
		return fromClause.fromElementSpace.getRoot().getIdentificationVariable().equals( identificationVariable )
				? fromClause.fromElementSpace.getRoot().getBinding()
				: null;
	}

	@Override
	public SqmNavigableReference findNavigableBindingExposingAttribute(String attributeName) {
		if ( rootExposesAttribute( attributeName ) ) {
			return fromClause.fromElementSpace.getRoot().getBinding();
		}
		else {
			return null;
		}
	}

	private boolean rootExposesAttribute(String attributeName) {
		return null != fromClause.fromElementSpace.getRoot().getBinding().getReferencedNavigable().findNavigable( attributeName );
	}

	@Override
	public FromElementLocator getFromElementLocator() {
		return this;
	}

	@Override
	public FromElementBuilder getFromElementBuilder() {
		return fromElementBuilder;
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

	public static class DmlFromElementBuilder extends FromElementBuilder {
		public DmlFromElementBuilder(ParsingContext parsingContext, AliasRegistry aliasRegistry) {
			super( parsingContext, aliasRegistry );
		}

		@Override
		public SqmCrossJoin makeCrossJoinedFromElement(
				SqmFromElementSpace fromElementSpace, String uid, SqmExpressableTypeEntity entityType, String alias) {
			throw new ParsingException( "DML from-clause cannot define joins" );
		}

		@Override
		public SqmEntityJoin buildEntityJoin(
				SqmFromElementSpace fromElementSpace,
				String alias,
				SqmExpressableTypeEntity entityType,
				SqmJoinType joinType) {
			throw new ParsingException( "DML from-clause cannot define joins" );
		}

		@Override
		public SqmAttributeJoin buildAttributeJoin(
				SqmAttributeReference attributeBinding,
				String alias,
				SqmExpressableTypeEntity subclassIndicator,
				SqmJoinType joinType,
				boolean fetched,
				boolean canReuseImplicitJoins) {
			if ( SqmExpressableTypeEmbedded.class.isInstance( attributeBinding.getReferencedNavigable() ) ) {
				return super.buildAttributeJoin(
						attributeBinding,
						alias,
						subclassIndicator,
						joinType,
						fetched,
						canReuseImplicitJoins
				);
			}
			throw new ParsingException( "DML from-clause cannot define joins" );
		}
	}
}
