/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.query;
import java.io.Serializable;
import javax.persistence.ColumnResult;
import javax.persistence.Entity;
import javax.persistence.EntityResult;
import javax.persistence.FieldResult;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.SqlResultSetMapping;

/**
 * @author Emmanuel Bernard
 */
@Entity
@IdClass(Identity.class)
@SqlResultSetMapping(name = "compositekey",
		entities = @EntityResult(entityClass = org.hibernate.test.annotations.query.SpaceShip.class,
				fields = {
				@FieldResult(name = "name", column = "name"),
				@FieldResult(name = "model", column = "model"),
				@FieldResult(name = "speed", column = "speed"),
				@FieldResult(name = "dimensions.width", column = "width"),
				@FieldResult(name = "captain.lastname", column = "lastn"),
				@FieldResult(name = "dimensions.length", column = "length"),
				@FieldResult(name = "captain.firstname", column = "firstn")
						}),
		columns = {@ColumnResult(name = "surface"),
		@ColumnResult(name = "volume")})
public class Captain implements Serializable {
	private String firstname;
	private String lastname;

	@Id
	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	@Id
	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}
}
