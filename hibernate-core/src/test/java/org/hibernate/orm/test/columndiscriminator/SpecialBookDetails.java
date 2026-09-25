package org.hibernate.orm.test.columndiscriminator;

public class SpecialBookDetails extends BookDetails {
	private String specialInformation;

	public SpecialBookDetails(String information, String specialInformation) {
		super(information);
		this.specialInformation = specialInformation;
	}

	protected SpecialBookDetails() {
		// default
	}

	public String specialInformation() {
		return specialInformation;
	}
}
