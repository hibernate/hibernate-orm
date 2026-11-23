package org.hibernate.orm.test.boot.jaxb.mapping.unmappedsuperclass;

public class AnotherEntity extends AbstractBase {
	private String code;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}
}
