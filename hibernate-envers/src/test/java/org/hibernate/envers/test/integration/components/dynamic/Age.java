package org.hibernate.envers.test.integration.components.dynamic;

public class Age {

	private int ageInYears;

	public Age() {
	}

	public Age(int ageInYears) {
		this.ageInYears = ageInYears;
	}

	public int getAgeInYears() {
		return ageInYears;
	}

	public void setAgeInYears(int ageInYears) {
		this.ageInYears = ageInYears;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !( o instanceof Age ) ) {
			return false;
		}

		Age age = (Age) o;

		if ( ageInYears != age.ageInYears ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return ageInYears;
	}

	@Override
	public String toString() {
		return "Age{" +
				"ageInYears=" + ageInYears +
				'}';
	}
}
