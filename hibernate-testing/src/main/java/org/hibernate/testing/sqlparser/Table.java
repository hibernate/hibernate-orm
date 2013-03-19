package org.hibernate.testing.sqlparser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Table extends Statement {

	public boolean cached;
	public String temporaryType;
	public boolean temporary;
	public boolean ifNotExists;
	public Name name;
	public List< Column > columns = new ArrayList< Column >();
	public List< Object > constraints = new ArrayList< Object >();
	public String onCommit;

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
		listToString( builder, columns );
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
