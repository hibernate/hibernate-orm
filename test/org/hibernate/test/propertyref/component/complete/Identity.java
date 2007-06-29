package org.hibernate.test.propertyref.component.complete;

import java.io.Serializable;

public class Identity implements Serializable {
	private String name;
	private String ssn;

	public String getSsn() {
		return ssn;
	}
	public void setSsn(String id) {
		this.ssn = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}
