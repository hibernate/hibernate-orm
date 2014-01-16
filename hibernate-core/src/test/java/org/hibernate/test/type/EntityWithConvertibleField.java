package org.hibernate.test.type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Oleksander Dukhno
 */
@Entity
public class EntityWithConvertibleField {

	private String id;
	private ConvertibleEnum testEnum;

	@Id
	@Column(name = "id")
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Column(name = "testEnum")
	public ConvertibleEnum getTestEnum() {
		return testEnum;
	}

	public void setTestEnum(ConvertibleEnum testEnum) {
		this.testEnum = testEnum;

	}

}
