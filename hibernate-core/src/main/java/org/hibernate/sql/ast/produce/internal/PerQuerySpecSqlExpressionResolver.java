/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.NonQualifiableSqlExpressable;
import org.hibernate.sql.ast.produce.spi.QualifiableSqlExpressable;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class PerQuerySpecSqlExpressionResolver implements SqlExpressionResolver {

	// todo (6.0) : allow sql expression/selection resolution up into containing QuerySpecs?

	private final SessionFactoryImplementor sessionFactory;
	private final Supplier<QuerySpec> querySpecSupplier;
	private final Function<Expression, Expression> normalizer;
	private final BiConsumer<Expression,SqlSelection> selectionConsumer;

	private final Map<QuerySpec,StandardSqlExpressionResolver> subResolverByQuerySpec = new HashMap<>();

	public PerQuerySpecSqlExpressionResolver(
			SessionFactoryImplementor sessionFactory,
			Supplier<QuerySpec> querySpecSupplier,
			Function<Expression,Expression> normalizer,
			BiConsumer<Expression,SqlSelection> selectionConsumer) {
		this.sessionFactory = sessionFactory;
		this.querySpecSupplier = querySpecSupplier;
		this.normalizer = normalizer;
		this.selectionConsumer = selectionConsumer;
	}

	@Override
	public Expression resolveSqlExpression(
			ColumnReferenceQualifier qualifier,
			QualifiableSqlExpressable sqlSelectable) {
		return determineSubResolver().resolveSqlExpression( qualifier, sqlSelectable );
	}

	private StandardSqlExpressionResolver determineSubResolver() {
		final QuerySpec querySpec = querySpecSupplier.get();
		return determineSubResolver( querySpec );
	}

	protected StandardSqlExpressionResolver determineSubResolver(QuerySpec querySpec) {
		if ( querySpec == null ) {
			throw new HibernateException( "No QuerySpec was supplied" );
		}

		return subResolverByQuerySpec.computeIfAbsent(
				querySpec,
				q -> new StandardSqlExpressionResolver( querySpecSupplier, normalizer, selectionConsumer )
		);
	}

	@Override
	public Expression resolveSqlExpression(NonQualifiableSqlExpressable sqlSelectable) {
		return determineSubResolver().resolveSqlExpression( sqlSelectable );
	}

	@Override
	public SqlSelection resolveSqlSelection(
			Expression expression,
			BasicJavaDescriptor javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {
		return determineSubResolver().resolveSqlSelection( expression, javaTypeDescriptor, typeConfiguration );
	}

	@Override
	public SqlSelection emptySqlSelection() {
		return determineSubResolver().emptySqlSelection();
	}
}
