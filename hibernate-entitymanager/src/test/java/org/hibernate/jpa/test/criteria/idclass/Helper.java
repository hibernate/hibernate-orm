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

import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.MappedSuperclass;
import javax.persistence.Table;

/**
 * @author Erich Heard
 */
@MappedSuperclass
@Table( name = "HELPER" )
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
