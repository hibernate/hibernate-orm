package org.hibernate.testing.sql;

public class Join extends AbstractSqlObject {

	public boolean left;
	public String type;
	public Aliasable aliasable;
	public SqlObject on;

	Join( SqlObject parent ) {
		super( parent );
	}

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
		builder.append( type == null ? "" : type ).append( " JOIN " ).append( aliasable ).append( " ON " ).append( on );
		return builder.toString();
	}
}
