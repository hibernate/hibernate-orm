package org.hibernate.testing.sql;

import java.util.ArrayList;
import java.util.List;

public class Insert extends DmlStatement {

	public List< Reference > columns = new OptionallyOrderedSet< Reference >();
	public List< SqlObject > values = new ArrayList< SqlObject >();
	public Select select;
	public SqlObject where;

	Insert() {
		super( null );
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder( "INSERT INTO " );
		if ( !tables.isEmpty() ) {
			builder.append( tables.get( 0 ) );
		}
		if ( !columns.isEmpty() ) {
			builder.append( ' ' );
			collectionToStringInParentheses( builder, columns );
		}
		if ( !values.isEmpty() ) {
			builder.append( " VALUES " );
			collectionToStringInParentheses( builder, values );
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
