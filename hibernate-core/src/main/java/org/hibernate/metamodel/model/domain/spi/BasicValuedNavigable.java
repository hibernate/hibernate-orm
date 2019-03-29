/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.Locale;
import java.util.function.BiConsumer;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.PhysicalColumn;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmNavigableReference;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.produce.SqlTreeException;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.DomainResultCreationException;
import org.hibernate.sql.results.internal.domain.basic.BasicResultImpl;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public interface BasicValuedNavigable<J> extends BasicValuedExpressableType<J>, Navigable<J>, SimpleTypeDescriptor<J>, BasicTypeDescriptor<J> {
	@Override
	default PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	Column getBoundColumn();

	BasicValueMapper<J> getValueMapper();

	@Override
	default BasicJavaDescriptor<J> getJavaTypeDescriptor() {
		return getValueMapper().getDomainJavaDescriptor();
	}

	@Override
	default SqlExpressableType getSqlExpressableType() {
		return getValueMapper().getSqlExpressableType();
	}

	@Override
	default Object unresolve(Object value, SharedSessionContractImplementor session) {
		return value;
	}

	@Override
	default void visitColumns(
			BiConsumer<SqlExpressableType, Column> action,
			Clause clause,
			TypeConfiguration typeConfiguration) {
		// todo (6.0) - formula based navigables have no bound column.
		//		this is a simple fix for now to avoid NPE
		//		we should more than likely make sure the boundColumn instance is a DerivedColumn?
		if ( getBoundColumn() != null ) {
			action.accept( getBoundColumn().getExpressableType(), getBoundColumn() );
		}
	}

	@Override
	default void dehydrate(
			Object value,
			JdbcValueCollector jdbcValueCollector,
			Clause clause,
			SharedSessionContractImplementor session) {
		final Column boundColumn = getBoundColumn();
		if ( boundColumn instanceof PhysicalColumn ) {
			jdbcValueCollector.collect( value, boundColumn.getExpressableType(), boundColumn );
		}
	}

	@Override
	default SqmNavigableReference createSqmExpression(SqmPath lhs, SqmCreationState creationState) {
		return new SqmBasicValuedSimplePath(
				lhs.getNavigablePath().append( getNavigableName() ),
				this,
				lhs
		);
	}

	@Override
	default DomainResult createDomainResult(
			NavigablePath navigablePath,
			String resultVariable,
			DomainResultCreationState creationState) {
		final NavigablePath parentNavigablePath = navigablePath.getParent();
		if ( parentNavigablePath == null ) {
			throw new DomainResultCreationException( "Parent NavigablePath cannot be null for basic path" );
		}

		final TableGroup tableGroup = creationState.getFromClauseAccess().findTableGroup( parentNavigablePath );
		if ( tableGroup == null ) {
			throw new SqlTreeException(
					String.format(
							Locale.ROOT,
							"Could not locate TableGroup for basic path : %s -> %s",
							navigablePath,
							this
					)
			);
		}

		// resolve the SqlSelection
		final SqlExpressionResolver resolver = creationState.getSqlExpressionResolver();
		final SqlSelection sqlSelection = resolver.resolveSqlSelection(
				resolver.resolveSqlExpression( tableGroup, getBoundColumn() ),
				getJavaTypeDescriptor(),
				creationState.getSqlAstCreationState().getCreationContext().getDomainModel().getTypeConfiguration()
		);

		return new BasicResultImpl( resultVariable, sqlSelection, getBoundColumn().getExpressableType() );
	}
}
