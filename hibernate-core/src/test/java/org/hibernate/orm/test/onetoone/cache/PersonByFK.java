package org.hibernate.orm.test.onetoone.cache;

public class PersonByFK extends Person {

	private Details details;

	@Override
	public Details getDetails() {
		return details;
	}

	@Override
	public void setDetails(Details details) {
		this.details = details;
	}
}
