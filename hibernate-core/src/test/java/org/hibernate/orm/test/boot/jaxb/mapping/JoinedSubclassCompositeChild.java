package org.hibernate.orm.test.boot.jaxb.mapping;

public class JoinedSubclassCompositeChild extends JoinedSubclassCompositeBase {
	private String detail;

	public String getDetail() {
		return detail;
	}

	public void setDetail(String detail) {
		this.detail = detail;
	}
}
