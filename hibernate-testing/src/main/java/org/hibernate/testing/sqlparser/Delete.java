package org.hibernate.testing.sqlparser;

public class Delete extends Statement {

	public Name table;
	public Object where;

	/**
	 * {@inheritDoc}
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder( "DELETE FROM " );
		builder.append( table );
		if ( where != null ) {
			builder.append( " WHERE " ).append( where );
		}
		return builder.toString();
	}
}
