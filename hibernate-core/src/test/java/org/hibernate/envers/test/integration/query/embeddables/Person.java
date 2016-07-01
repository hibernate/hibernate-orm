/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.query.embeddables;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

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
