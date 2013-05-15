package org.hibernate.testing.sql;

import java.util.ArrayList;
import java.util.List;

public class Select extends DmlStatement {

	public boolean distinct;
	public List< SqlObject > columns = new OptionallyOrderedSet< SqlObject >();
	public List< From > froms = new OptionallyOrderedSet< From >();
	public SqlObject where;
	public Reference groupBy;
	public SqlObject having;
	public List< OrderBy > orderBy = new ArrayList< OrderBy >();
	public SqlObject limit;
	public SqlObject offset;
	public boolean forUpdate;
	public Select union;
	public boolean unionAll;

	Select( SqlObject parent ) {
		super( parent );
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder( "SELECT " );
		if ( distinct ) {
			builder.append( "DISTINCT " );
		}
		collectionToString( builder, columns );
		if ( !froms.isEmpty() ) {
			builder.append( " FROM " );
			for ( From from : froms ) {
				builder.append( from );
			}
		}
		if ( where != null ) {
			builder.append( " WHERE " ).append( where );
		}
		if ( groupBy != null ) {
			builder.append( " GROUP BY " ).append( groupBy );
		}
		if ( having != null ) {
			builder.append( " HAVING " ).append( having );
		}
		if ( !orderBy.isEmpty() ) {
			builder.append( " ORDER BY " );
			collectionToString( builder, orderBy );
		}
		if ( limit != null ) {
			builder.append( " LIMIT " ).append( limit );
		}
		if ( offset != null ) {
			builder.append( " OFFSET " ).append( offset );
		}
		if ( forUpdate ) {
			builder.append( " FOR UPDATE" );
		}
		if ( union != null ) {
			builder.append( " UNION " );
			if ( unionAll ) {
				builder.append( "ALL " );
			}
			builder.append( union );
		}
		return builder.toString();
	}
}
