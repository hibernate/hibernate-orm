/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.ops;


/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class PersonalDetails {
	private Long id;
	private String somePersonalDetail;
	private Person person;

	public PersonalDetails() {
	}

	public PersonalDetails(String somePersonalDetail, Person person) {
		this.somePersonalDetail = somePersonalDetail;
		this.person = person;
		person.setDetails( this );
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getSomePersonalDetail() {
		return somePersonalDetail;
	}

	public void setSomePersonalDetail(String somePersonalDetail) {
		this.somePersonalDetail = somePersonalDetail;
	}

	public Person getPerson() {
		return person;
	}

	public void setPerson(Person person) {
		this.person = person;
	}
}
