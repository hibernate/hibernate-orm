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
 * @author Gail Badner
 */

@Embeddable
public class HelperId implements Serializable {
	private static final long serialVersionUID = 9122480802791185646L;
	
	@Id
	@Column( name = "NAME", length = 12 )
	private String name;
	public String getName( ) { return this.name; }
	public void setName( String value ) { this.name = value; }
	
	@Id
	@Column( name = "HELPER_TYPE", length = 4 )
	private String type;
	public String getType( ) { return this.type; }
	public void setType( String value ) { this.type = value; }
	
	@Override
	public boolean equals( Object obj ) {
		if( obj == null ) return false;
		if( !( obj instanceof HelperId ) ) return false;
		
		HelperId id = ( HelperId )obj;
		if( this.getName() == null || id.getName() == null || this.getType() == null || id.getType() == null ) return false;
		
		return this.toString( ).equals( id.toString( ) );
	}
	
	@Override
	public int hashCode( ) {
		return this.toString( ).hashCode( );
	}
	
	@Override
	public String toString( ) {
		StringBuilder buf = new StringBuilder( "[id:" );
		buf.append( ( this.getName() == null ) ? "null" : this.getName( ) );
		buf.append( ";type:" );
		buf.append( ( this.getType() == null ) ? "null" : this.getType() );
		buf.append( "]" );
		
		return buf.toString( );
	}
}
