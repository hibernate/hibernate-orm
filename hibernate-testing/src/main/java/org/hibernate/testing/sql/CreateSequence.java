package org.hibernate.testing.sql;

public class CreateSequence extends DdlStatement implements NamedObject {

	public Name name;
	public Integer start;
	public int increment = 1;

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.testing.sql.NamedObject#name()
	 */
	@Override
	public Name name() {
		return name;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.hibernate.testing.sql.NamedObject#setName(org.hibernate.testing.sql.Name)
	 */
	@Override
	public void setName( Name name ) {
		this.name = name;
	}

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
