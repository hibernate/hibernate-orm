package org.hibernate.testing.sql;

import java.util.Iterator;
import java.util.List;

public class CreateTable extends DdlStatement implements NamedObject {

	public Name name;
	public boolean cached;
	public String temporaryType;
	public boolean temporary;
	public boolean ifNotExists;
	public List< Column > columns = new OptionallyOrderedSet< Column >();
	public List< SqlObject > constraints = new OptionallyOrderedSet< SqlObject >();
	public String onCommit;

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
		StringBuilder builder = new StringBuilder( "CREATE " );
		if ( cached ) {
			builder.append( "CACHED " );
		}
		if ( temporaryType != null ) {
			builder.append( temporaryType ).append( ' ' );
		}
		if ( temporary ) {
			builder.append( "TEMPORARY " );
		}
		if ( ifNotExists ) {
			builder.append( "IF NOT EXISTS " );
		}
		builder.append( "TABLE " ).append( name ).append( " ( " );
		collectionToString( builder, columns );
		for ( Iterator< ? > iter = constraints.iterator(); iter.hasNext(); ) {
			builder.append( ", " ).append( iter.next() );
		}
		builder.append( " )" );
		if ( onCommit != null ) {
			builder.append( " ON COMMIT " ).append( onCommit );
		}
		return builder.toString();
	}
}
