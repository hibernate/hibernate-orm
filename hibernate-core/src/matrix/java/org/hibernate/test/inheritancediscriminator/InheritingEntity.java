package org.hibernate.test.inheritancediscriminator;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Created by Pawel Stawicki on 8/17/11 11:01 PM
 */
@Entity
@DiscriminatorValue("1")
public class InheritingEntity extends ParentEntity {

	@Column(name = "dupa")
	private String someValue;

	public String getSomeValue() {
		return someValue;
	}

	public void setSomeValue(String someValue) {
		this.someValue = someValue;
	}
}
