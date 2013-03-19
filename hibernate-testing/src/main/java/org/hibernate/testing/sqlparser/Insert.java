package org.hibernate.testing.sqlparser;

import java.util.ArrayList;
import java.util.List;

public class Insert extends Statement {

	public Name table;
	public List< Name > columns = new ArrayList< Name >();
	public List< Object > values = new ArrayList< Object >();
	public Select select;
	public Object where;

	/**
	 * {@inheritDoc}
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder( "INSERT INTO " );
		builder.append( table );
		if ( !columns.isEmpty() ) {
			builder.append( ' ' );
			listToStringInParentheses( builder, columns );
		}
		if ( !values.isEmpty() ) {
			builder.append( " VALUES " );
			listToStringInParentheses( builder, values );
		}
		if ( select != null ) {
			builder.append( " ( " ).append( select ).append( " )" );
		}
		if ( where != null ) {
			builder.append( " WHERE " ).append( where );
		}
		return builder.toString();
	}
}
