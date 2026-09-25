package org.hibernate.sql.ast.spi.query.expression;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.spi.IndexedConsumer;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SqlExpressible;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAstNode;

/**
 * @author Christian Beikov
 */
public class Collation implements SqlExpressible, SqlAstNode {

	private final String collation;

	public Collation(String collation) {
		this.collation = collation;
	}

	public String getCollation() {
		return collation;
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitCollation( this );
	}

	@Override
	public int forEachJdbcType(int offset, @Nonnull IndexedConsumer<JdbcMapping> action) {
		return 0;
	}

	@Nullable
	@Override
	public JdbcMapping getJdbcMapping() {
		return null;
	}
}
