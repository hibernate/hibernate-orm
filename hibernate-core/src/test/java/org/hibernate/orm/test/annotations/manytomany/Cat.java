/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytomany;
import java.util.Set;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "tbl_cat")
//ANN-630
//@org.hibernate.annotations.Table(appliesTo= "TT", indexes = @Index(name = "testidx", columnNames = "cat_id"))
public class Cat {
	private CatPk id;
	private int age;
	private Set<Woman> humanContacts;

	@ManyToMany
	//@Index(name = "CAT_HUMAN_IDX")
	@JoinTable(name="TT")
	public Set<Woman> getHumanContacts() {
		return humanContacts;
	}

	public void setHumanContacts(Set<Woman> humanContacts) {
		this.humanContacts = humanContacts;
	}

	@EmbeddedId()
	public CatPk getId() {
		return id;
	}

	public void setId(CatPk id) {
		this.id = id;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof Cat ) ) return false;

		final Cat cat = (Cat) o;

		if ( !id.equals( cat.id ) ) return false;

		return true;
	}

	public int hashCode() {
		return id.hashCode();
	}
}
