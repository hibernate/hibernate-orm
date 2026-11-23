package org.hibernate.orm.test.boot.jaxb.mapping;

public class JoinedSubclassFkChild extends JoinedSubclassFkBase {
	private String detail;

	public String getDetail() {
		return detail;
	}

	public void setDetail(String detail) {
		this.detail = detail;
	}
}
