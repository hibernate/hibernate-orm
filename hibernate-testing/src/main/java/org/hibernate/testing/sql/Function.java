package org.hibernate.testing.sql;

import java.util.List;

public class Function extends AbstractSqlObject {

	public String name;
	public List< SqlObject > parameters = new OptionallyOrderedSet< SqlObject >();

	Function( SqlObject parent, String name ) {
		super( parent );
		this.name = name;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder( name );
		collectionToStringInParentheses( builder, parameters );
		return builder.toString();
	}
}
