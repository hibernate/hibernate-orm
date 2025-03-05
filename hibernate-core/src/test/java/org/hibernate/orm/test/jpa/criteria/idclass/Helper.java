/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.idclass;

import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.MappedSuperclass;

/**
 * @author Erich Heard
 */
@MappedSuperclass
@IdClass( HelperId.class )
public class Helper {
	@Id
	private String name;
	public String getName( ) { return this.name; }
	public void setName( String value ) { this.name = value; }

	@Id
	private String type;
	public String getType( ) { return this.type; }
	public void setType( String value ) { this.type = value; }

	@Override
	public String toString( ) {
		return "[Name:" + this.getName( ) + "; Type: " + this.getType( ) + "]";
	}
}
