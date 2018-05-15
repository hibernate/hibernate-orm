/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria.idclass;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Id;

/**
 * @author Erich Heard
 */

@Embeddable
public class WidgetId implements Serializable {
	private static final long serialVersionUID = 9122480802791185644L;
	
	@Id
	@Column( name = "CODE", length = 3 )
	private String code;
	public String getCode( ) { return this.code; }
	public void setCode( String value ) { this.code = value; }
	
	@Id
	@Column( name = "DIVISION", length = 4 )
	private String division;
	public String getDivision( ) { return this.division; }
	public void setDivision( String value ) { this.division = value; }
	
	@Override
	public boolean equals( Object obj ) {
		if( obj == null ) return false;
		if( !( obj instanceof WidgetId ) ) return false;
		
		WidgetId id = ( WidgetId )obj;
		if( this.getCode( ) == null || id.getCode( ) == null || this.getDivision( ) == null || id.getDivision( ) == null ) return false;
		
		return this.toString( ).equals( id.toString( ) );
	}
	
	@Override
	public int hashCode( ) {
		return this.toString( ).hashCode( );
	}
	
	@Override
	public String toString( ) {
		StringBuffer buf = new StringBuffer( "[id:" );
		buf.append( ( this.getCode( ) == null ) ? "null" : this.getCode( ).toString( ) );
		buf.append( ";code:" );
		buf.append( ( this.getDivision( ) == null ) ? "null" : this.getDivision( ) );
		buf.append( "]" );
		
		return buf.toString( );
	}
}
