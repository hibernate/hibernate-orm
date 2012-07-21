/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
