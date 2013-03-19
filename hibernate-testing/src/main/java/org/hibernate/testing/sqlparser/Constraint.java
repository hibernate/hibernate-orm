package org.hibernate.testing.sqlparser;

import java.util.ArrayList;
import java.util.List;

public abstract class Constraint {

	public Name name;
	public List< Name > columns = new ArrayList< Name >();

	public abstract String type();

	/**
	 * {@inheritDoc}
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if ( name != null ) {
			builder.append( "CONSTRAINT " ).append( name ).append( ' ' );
		}
		builder.append( type() ).append( ' ' );
		Statement.listToStringInParentheses( builder, columns );
		return builder.toString();
	}
}
