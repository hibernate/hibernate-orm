package org.hibernate.orm.test.jcache.domain;

public class Account {

	private Long id;
	private Person person;

	public Account() {
		//
	}

	public Person getPerson() {
		return person;
	}

	public void setPerson(Person person) {
		this.person = person;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String toString() {
		return super.toString();
	}
}
