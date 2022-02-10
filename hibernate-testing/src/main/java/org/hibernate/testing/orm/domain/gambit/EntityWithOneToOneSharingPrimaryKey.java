/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.gambit;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

/**
 * @author Andrea Boriero
 */
@Entity
@Table(name = "entity_o2o_sharepk")
public class EntityWithOneToOneSharingPrimaryKey {
	private Integer id;

	// alphabetical
	private String name;
	private SimpleEntity other;
	private Integer someInteger;

	public EntityWithOneToOneSharingPrimaryKey() {
	}

	public EntityWithOneToOneSharingPrimaryKey(Integer id, String name, Integer someInteger) {
		this.id = id;
		this.name = name;
		this.someInteger = someInteger;
	}

	@Id
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

	@OneToOne
	@PrimaryKeyJoinColumn
	public SimpleEntity getOther() {
		return other;
	}

	public void setOther(SimpleEntity other) {
		this.other = other;
	}

	public Integer getSomeInteger() {
		return someInteger;
	}

	public void setSomeInteger(Integer someInteger) {
		this.someInteger = someInteger;
	}
}
