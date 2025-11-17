/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query.embeddables;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Entity
@Audited
public class Person {
	@Id
	@GeneratedValue
	private Integer id;
	private String name;
	private NameInfo nameInfo;

	Person() {

	}

	public Person(String name, NameInfo nameInfo) {
		this.name = name;
		this.nameInfo = nameInfo;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public NameInfo getNameInfo() {
		return nameInfo;
	}

	public void setNameInfo(NameInfo nameInfo) {
		this.nameInfo = nameInfo;
	}

	@Override
	public int hashCode() {
		int result;
		result = ( id != null ? id.hashCode() : 0 );
		result = 31 * result + ( name != null ? name.hashCode() : 0 );
		result = 31 * result + ( nameInfo != null ? nameInfo.hashCode() : 0 );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !( obj instanceof Person ) ) {
			return false;
		}
		Person that = (Person) obj;
		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}
		if ( name != null ? !name.equals( that.name) : that.name != null ) {
			return false;
		}
		if ( nameInfo != null ? !nameInfo.equals( that.nameInfo ) : that.nameInfo != null ) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "Person{" +
				"id=" + id +
				", name='" + name + '\'' +
				", nameInfo=" + nameInfo +
				'}';
	}
}
