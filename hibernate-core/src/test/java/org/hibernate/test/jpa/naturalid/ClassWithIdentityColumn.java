package org.hibernate.test.jpa.naturalid;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.annotations.NaturalId;

@Entity
public class ClassWithIdentityColumn {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@NaturalId(mutable = true)
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
