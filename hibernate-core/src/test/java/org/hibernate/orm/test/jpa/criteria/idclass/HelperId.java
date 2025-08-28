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
