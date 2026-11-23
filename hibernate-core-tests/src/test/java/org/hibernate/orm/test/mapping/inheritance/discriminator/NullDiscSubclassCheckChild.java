package org.hibernate.orm.test.mapping.inheritance.discriminator;

public class NullDiscSubclassCheckChild extends NullDiscSubclassCheckBase {
	private String requiredProp;

	public String getRequiredProp() {
		return requiredProp;
	}

	public void setRequiredProp(String requiredProp) {
		this.requiredProp = requiredProp;
	}
}
