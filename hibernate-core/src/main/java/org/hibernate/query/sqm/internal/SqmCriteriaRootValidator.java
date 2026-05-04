/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.internal;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.List;

import org.hibernate.metamodel.model.domain.internal.AnyDiscriminatorSqmPath;
import org.hibernate.query.sqm.DiscriminatorSqmPath;
import org.hibernate.query.sqm.spi.BaseSemanticQueryWalker;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.domain.NonAggregatedCompositeSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmAnyValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmCorrelation;
import org.hibernate.query.sqm.tree.domain.SqmCteRoot;
import org.hibernate.query.sqm.tree.domain.SqmDerivedRoot;
import org.hibernate.query.sqm.tree.domain.SqmElementAggregateFunction;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddedValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmEntityValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmFkExpression;
import org.hibernate.query.sqm.tree.domain.SqmFunctionPath;
import org.hibernate.query.sqm.tree.domain.SqmFunctionRoot;
import org.hibernate.query.sqm.tree.domain.SqmIndexAggregateFunction;
import org.hibernate.query.sqm.tree.domain.SqmIndexedCollectionAccessPath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmPluralPartJoin;
import org.hibernate.query.sqm.tree.domain.SqmPluralValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmTreatedPath;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmCteJoin;
import org.hibernate.query.sqm.tree.from.SqmDerivedJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFunctionJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.select.SqmQueryPart;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Validates that an SQM tree constructed via the criteria API tree does
 * not contain path nodes referring to roots from a different SQM tree.
 * <p>
 * Criteria nodes retain object identity links to the {@link SqmRoot}
 * against which they were created. Reusing a node in a different query
 * tree can otherwise fail later during SQL AST conversion with a less
 * specific table-group lookup error. This walker checks those links while
 * the criteria tree is being validated.
 * <p>
 * The check is limited to paths that resolve to an SQM root. Legal
 * rootless paths, such as function paths, are ignored for this purpose,
 * and wrapper paths such as {@link SqmTreatedPath} are unwrapped before
 * roots are compared.
 *
 * @author Gavin King
 * @since 7.4
 */
final class SqmCriteriaRootValidator extends BaseSemanticQueryWalker {
	private final ArrayDeque<Collection<? extends SqmRoot<?>>> validRootStack = new ArrayDeque<>();

	static void validate(SqmQueryPart<?> queryPart) {
		new SqmCriteriaRootValidator().visitQueryPart( queryPart );
	}

	static void validate(SqmDeleteStatement<?> statement) {
		new SqmCriteriaRootValidator().visitDeleteStatement( statement );
	}

	static void validate(SqmUpdateStatement<?> statement) {
		new SqmCriteriaRootValidator().visitUpdateStatement( statement );
	}

	@Override
	public Object visitUpdateStatement(SqmUpdateStatement<?> statement) {
		visitCteContainer( statement );
		final var roots = List.of( statement.getTarget() );
		validRootStack.push( roots );
		try {
			visitSetClause( statement.getSetClause() );
			visitWhereClause( statement.getWhereClause() );
			return statement;
		}
		finally {
			validRootStack.pop();
		}
	}

	@Override
	public Object visitDeleteStatement(SqmDeleteStatement<?> statement) {
		visitCteContainer( statement );
		final var roots = List.of( statement.getTarget() );
		validRootStack.push( roots );
		try {
			visitWhereClause( statement.getWhereClause() );
			return statement;
		}
		finally {
			validRootStack.pop();
		}
	}

	@Override
	public Object visitQuerySpec(SqmQuerySpec<?> querySpec) {
		final var roots = querySpec.getFromClause().getRoots();
		validRootStack.push( roots );
		try {
			return super.visitQuerySpec( querySpec );
		}
		finally {
			validRootStack.pop();
		}
	}

	@Override
	public Object visitRootPath(SqmRoot<?> sqmRoot) {
		validateRoot( sqmRoot, sqmRoot );
		return super.visitRootPath( sqmRoot );
	}

	@Override
	public Object visitRootDerived(SqmDerivedRoot<?> sqmRoot) {
		validateRoot( sqmRoot, sqmRoot );
		return super.visitRootDerived( sqmRoot );
	}

	@Override
	public Object visitRootFunction(SqmFunctionRoot<?> sqmRoot) {
		validateRoot( sqmRoot, sqmRoot );
		return super.visitRootFunction( sqmRoot );
	}

	@Override
	public Object visitRootCte(SqmCteRoot<?> sqmRoot) {
		validateRoot( sqmRoot, sqmRoot );
		return super.visitRootCte( sqmRoot );
	}

	@Override
	public Object visitCrossJoin(SqmCrossJoin<?> joinedFromElement) {
		validatePath( joinedFromElement );
		return super.visitCrossJoin( joinedFromElement );
	}

	@Override
	public Object visitPluralPartJoin(SqmPluralPartJoin<?, ?> joinedFromElement) {
		validatePath( joinedFromElement );
		return super.visitPluralPartJoin( joinedFromElement );
	}

	@Override
	public Object visitQualifiedEntityJoin(SqmEntityJoin<?, ?> joinedFromElement) {
		validatePath( joinedFromElement );
		return super.visitQualifiedEntityJoin( joinedFromElement );
	}

	@Override
	public Object visitQualifiedAttributeJoin(SqmAttributeJoin<?, ?> joinedFromElement) {
		validatePath( joinedFromElement );
		return super.visitQualifiedAttributeJoin( joinedFromElement );
	}

	@Override
	public Object visitQualifiedDerivedJoin(SqmDerivedJoin<?> joinedFromElement) {
		validatePath( joinedFromElement );
		return super.visitQualifiedDerivedJoin( joinedFromElement );
	}

	@Override
	public Object visitQualifiedFunctionJoin(SqmFunctionJoin<?> joinedFromElement) {
		joinedFromElement.getFunction().accept( this );
		return super.visitQualifiedFunctionJoin( joinedFromElement );
	}

	@Override
	public Object visitQualifiedCteJoin(SqmCteJoin<?> joinedFromElement) {
		validatePath( joinedFromElement );
		return super.visitQualifiedCteJoin( joinedFromElement );
	}

	@Override
	public Object visitBasicValuedPath(SqmBasicValuedSimplePath<?> path) {
		validatePath( path );
		return super.visitBasicValuedPath( path );
	}

	@Override
	public Object visitEmbeddableValuedPath(SqmEmbeddedValuedSimplePath<?> path) {
		validatePath( path );
		return super.visitEmbeddableValuedPath( path );
	}

	@Override
	public Object visitAnyValuedValuedPath(SqmAnyValuedSimplePath<?> path) {
		validatePath( path );
		return super.visitAnyValuedValuedPath( path );
	}

	@Override
	public Object visitNonAggregatedCompositeValuedPath(NonAggregatedCompositeSimplePath<?> path) {
		validatePath( path );
		return super.visitNonAggregatedCompositeValuedPath( path );
	}

	@Override
	public Object visitEntityValuedPath(SqmEntityValuedSimplePath<?> path) {
		validatePath( path );
		return super.visitEntityValuedPath( path );
	}

	@Override
	public Object visitPluralValuedPath(SqmPluralValuedSimplePath<?> path) {
		validatePath( path );
		return super.visitPluralValuedPath( path );
	}

	@Override
	public Object visitFkExpression(SqmFkExpression<?> fkExpression) {
		validatePath( fkExpression );
		return super.visitFkExpression( fkExpression );
	}

	@Override
	public Object visitDiscriminatorPath(DiscriminatorSqmPath<?> sqmPath) {
		validatePath( sqmPath );
		return super.visitDiscriminatorPath( sqmPath );
	}

	@Override
	public Object visitIndexedPluralAccessPath(SqmIndexedCollectionAccessPath<?> path) {
		validatePath( path );
		return super.visitIndexedPluralAccessPath( path );
	}

	@Override
	public Object visitElementAggregateFunction(SqmElementAggregateFunction<?> path) {
		validatePath( path );
		return super.visitElementAggregateFunction( path );
	}

	@Override
	public Object visitIndexAggregateFunction(SqmIndexAggregateFunction<?> path) {
		validatePath( path );
		return super.visitIndexAggregateFunction( path );
	}

	@Override
	public Object visitFunctionPath(SqmFunctionPath<?> functionPath) {
		return super.visitFunctionPath( functionPath );
	}

	@Override
	public Object visitTreatedPath(SqmTreatedPath<?, @Nullable ?> sqmTreatedPath) {
		validatePath( sqmTreatedPath );
		return super.visitTreatedPath( sqmTreatedPath );
	}

	@Override
	public Object visitCorrelation(SqmCorrelation<?, ?> correlation) {
		validatePath( correlation );
		return super.visitCorrelation( correlation );
	}

	@Override
	public Object visitAnyDiscriminatorTypeExpression(AnyDiscriminatorSqmPath<?> expression) {
		validatePath( expression );
		return super.visitAnyDiscriminatorTypeExpression( expression );
	}

	private void validatePath(SqmPath<?> path) {
		final var root = findRoot( path );
		if ( root != null ) {
			validateRoot( root, path );
		}
	}

	private @Nullable SqmRoot<?> findRoot(SqmPath<?> path) {
		if ( path instanceof SqmTreatedPath<?, ?> treatedPath ) {
			return findRoot( treatedPath.getWrappedPath() );
		}
		else if ( path instanceof SqmRoot<?> root ) {
			return root;
		}
		else {
			final var lhs = path.getLhs();
			return lhs == null ? null : findRoot( lhs );
		}
	}

	private void validateRoot(SqmRoot<?> root, SqmPath<?> path) {
		for ( var validRoots : validRootStack ) {
			if ( validRoots.contains( root ) ) {
				return;
			}
		}
		throw new IllegalStateException(
				"Criteria path '" + path.getNavigablePath().getFullPath() + "' references a root from a different tree "
					+ " (criteria nodes may only be used within the same query or subquery in which they were created)"
		);
	}
}
