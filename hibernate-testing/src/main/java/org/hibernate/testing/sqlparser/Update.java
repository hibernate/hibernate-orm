package org.hibernate.testing.sqlparser;

import java.util.ArrayList;
import java.util.List;

public class Update extends Statement {

	public String table;
	public List< Object > sets = new ArrayList< Object >();
	public Object where;

	/**
	 * {@inheritDoc}
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder( "UPDATE " );
		builder.append( table ).append( " SET " );
		listToString( builder, sets );
		if ( where != null ) {
			builder.append( " WHERE " ).append( where );
		}
		return builder.toString();
	}
}
