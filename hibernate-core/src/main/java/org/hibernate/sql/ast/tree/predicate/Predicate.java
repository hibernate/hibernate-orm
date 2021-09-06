/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.predicate;

import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlAstTreeHelper;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Models a predicate in the SQL AST
 *
 * @author Steve Ebersole
 */
public interface Predicate extends Expression, DomainResultProducer<Boolean> {
	/**
	 * Short-cut for {@link SqlAstTreeHelper#combinePredicates}
	 */
	static Predicate combinePredicates(Predicate p1, Predicate p2) {
		return SqlAstTreeHelper.combinePredicates( p1, p2 );
	}

	boolean isEmpty();

	@Override
	default DomainResult<Boolean> createDomainResult(String resultVariable, DomainResultCreationState creationState) {
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();
		final JavaTypeDescriptor javaTypeDescriptor = getExpressionType().getJdbcMappings().get( 0 ).getJavaTypeDescriptor();
		final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
				this,
				javaTypeDescriptor,
				sqlAstCreationState.getCreationContext().getDomainModel().getTypeConfiguration()
		);

		//noinspection unchecked
		return new BasicResult( sqlSelection.getValuesArrayPosition(), resultVariable, javaTypeDescriptor );
	}
}
