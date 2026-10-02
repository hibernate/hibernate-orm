package org.hibernate.orm.test.boot.models.xml.column.transform.attributeoverride;

public class Measurement {
	private double valueInInches;

	public double getValueInInches() {
		return valueInInches;
	}

	public void setValueInInches(double valueInInches) {
		this.valueInInches = valueInInches;
	}
}
