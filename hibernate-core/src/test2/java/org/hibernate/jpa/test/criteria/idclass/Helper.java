/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
