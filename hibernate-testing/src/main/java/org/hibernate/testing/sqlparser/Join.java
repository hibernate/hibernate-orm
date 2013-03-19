package org.hibernate.testing.sqlparser;

public class Join {

	public boolean left;
	public String type;
	public Aliasable table;
	public Object on;

	/**
	 * {@inheritDoc}
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if ( left ) {
			builder.append( "LEFT " );
		}
		builder.append( type == null ? "" : type ).append( " JOIN " ).append( table ).append( " ON " ).append( on );
		return builder.toString();
	}
}
