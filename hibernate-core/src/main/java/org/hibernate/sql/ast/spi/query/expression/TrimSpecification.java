package org.hibernate.sql.ast.spi.query.expression;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.spi.IndexedConsumer;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SqlExpressible;
import org.hibernate.query.sqm.TrimSpec;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAstNode;

/**
 * @author Gavin King
 */
public class TrimSpecification implements SqlExpressible, SqlAstNode {
	private final TrimSpec trimSpec;

	public TrimSpecification(TrimSpec trimSpec) {
		this.trimSpec = trimSpec;
	}

	public TrimSpec getSpecification() {
		return trimSpec;
	}

	@Nullable
	@Override
	public JdbcMapping getJdbcMapping() {
		return null;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitTrimSpecification( this );
	}

	@Override
	public int forEachJdbcType(int offset, @Nonnull IndexedConsumer<JdbcMapping> action) {
		action.accept( offset, getJdbcMapping() );
		return getJdbcTypeCount();
	}
}
