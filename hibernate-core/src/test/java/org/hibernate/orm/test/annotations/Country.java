/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations;
import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity()
public class Country implements Serializable {
	private Integer id;
	private String name;

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setId(Integer integer) {
		id = integer;
	}

	public void setName(String string) {
		name = string;
	}

	public int hashCode() {
		return name == null ? 0 : name.hashCode();
	}

	public boolean equals(Object obj) {
		if ( obj == this ) return true;
		if ( ! ( obj instanceof Country ) ) return false;
		Country that = (Country) obj;
		if ( this.name == null ) return false;
		return this.name.equals( that.name );
	}
}
