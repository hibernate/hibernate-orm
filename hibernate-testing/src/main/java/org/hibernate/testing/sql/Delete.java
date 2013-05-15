package org.hibernate.testing.sql;

public class Delete extends DmlStatement {

	public SqlObject where;

	Delete() {
		super( null );
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder( "DELETE FROM " );
		if ( !tables.isEmpty() ) {
			builder.append( tables.get( 0 ) );
		}
		if ( where != null ) {
			builder.append( " WHERE " ).append( where );
		}
		return builder.toString();
	}
}
