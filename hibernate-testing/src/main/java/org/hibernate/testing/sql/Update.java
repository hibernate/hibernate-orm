package org.hibernate.testing.sql;

import java.util.List;

public class Update extends DmlStatement {

	public List< SqlObject > sets = new OptionallyOrderedSet< SqlObject >();
	public SqlObject where;

	Update() {
		super( null );
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder( "UPDATE " );
		if ( !tables.isEmpty() ) {
			builder.append( tables.get( 0 ) );
		}
		builder.append( " SET " );
		collectionToString( builder, sets );
		if ( where != null ) {
			builder.append( " WHERE " ).append( where );
		}
		return builder.toString();
	}
}
