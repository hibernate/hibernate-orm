package org.hibernate.testing.sqlparser;

public class Sequence extends Statement {

	public Name name;
	public Integer start;
	public int increment = 1;

	/**
	 * {@inheritDoc}
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder( "CREATE SEQUENCE " );
		builder.append( name );
		if ( start != null ) {
			builder.append( " START WITH " ).append( start );
		}
		if ( increment != 1 ) {
			builder.append( " INCREMENT BY " ).append( increment );
		}
		return builder.toString();
	}
}
