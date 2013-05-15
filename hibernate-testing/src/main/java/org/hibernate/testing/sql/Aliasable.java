package org.hibernate.testing.sql;

public class Aliasable extends AbstractSqlObject {

	public SqlObject expression;
	public Alias alias;

	Aliasable( SqlObject parent ) {
		super( parent );
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return alias == null ? ( expression == null ? "" : expression.toString() ) : expression + " AS " + alias;
	}
}
