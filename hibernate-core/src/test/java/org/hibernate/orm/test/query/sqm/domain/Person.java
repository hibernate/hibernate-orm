/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.sqm.domain;

import java.time.Instant;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * @author Steve Ebersole
 */
@Entity
public class Person {
	private Integer pk;
	private Name name;
	private String nickName;
	private Instant dob;
	private int numberOfToes;

	private Person mate;

	@Id
	public Integer getPk() {
		return pk;
	}

	public void setPk(Integer pk) {
		this.pk = pk;
	}

	public Name getName() {
		return name;
	}

	public void setName(Name name) {
		this.name = name;
	}

	public String getNickName() {
		return nickName;
	}

	public void setNickName(String nickName) {
		this.nickName = nickName;
	}

	@Temporal( TemporalType.TIMESTAMP )
	public Instant getDob() {
		return dob;
	}

	public void setDob(Instant dob) {
		this.dob = dob;
	}

	public int getNumberOfToes() {
		return numberOfToes;
	}

	public void setNumberOfToes(int numberOfToes) {
		this.numberOfToes = numberOfToes;
	}

	@ManyToOne
	@JoinColumn
	public Person getMate() {
		return mate;
	}

	public void setMate(Person mate) {
		this.mate = mate;
	}

	@Embeddable
	public static class Name {
		public String firstName;
		public String lastName;

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}
	}
}
