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
