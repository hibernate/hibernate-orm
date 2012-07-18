package org.hibernate.jpa.test.xml;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class Qualifier {
	private Long qualifierId;
	private String name;
	private String value;

	public Long getQualifierId() {
		return qualifierId;
	}

	public void setQualifierId(Long qualifierId) {
		this.qualifierId = qualifierId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
