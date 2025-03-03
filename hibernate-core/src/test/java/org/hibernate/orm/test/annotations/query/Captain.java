/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.query;
import java.io.Serializable;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FieldResult;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.SqlResultSetMapping;

/**
 * @author Emmanuel Bernard
 */
@Entity
@IdClass(Identity.class)
@SqlResultSetMapping(name = "compositekey",
		entities = @EntityResult(entityClass = SpaceShip.class,
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
