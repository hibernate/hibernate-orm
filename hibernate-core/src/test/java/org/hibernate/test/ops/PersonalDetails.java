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
