/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.idclass;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Id;

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
		StringBuilder buf = new StringBuilder( "[id:" );
		buf.append( ( this.getCode( ) == null ) ? "null" : this.getCode( ).toString( ) );
		buf.append( ";code:" );
		buf.append( ( this.getDivision( ) == null ) ? "null" : this.getDivision( ) );
		buf.append( "]" );

		return buf.toString( );
	}
}
