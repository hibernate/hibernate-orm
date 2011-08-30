package org.hibernate.test.component.cascading.toone;


/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class User {
	private Long id;
	private PersonalInfo personalInfo;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public PersonalInfo getPersonalInfo() {
		return personalInfo;
	}

	public void setPersonalInfo(PersonalInfo personalInfo) {
		this.personalInfo = personalInfo;
	}
}
