/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm.produce.domain;

import java.time.Instant;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

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
		public String first;
		public String last;

		public String getFirst() {
			return first;
		}

		public void setFirst(String first) {
			this.first = first;
		}

		public String getLast() {
			return last;
		}

		public void setLast(String last) {
			this.last = last;
		}
	}
}
