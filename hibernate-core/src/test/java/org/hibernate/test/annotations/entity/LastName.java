package org.hibernate.test.annotations.entity;
import javax.persistence.Embeddable;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;


@TypeDef(
		name = "upperCase",
		typeClass = CasterStringType.class,
		parameters = {
			@Parameter(name = "cast", value = "upper")
		}
)

@Embeddable
public class LastName {
	
	@Type(type="upperCase")
	private String lastName;

	public String getName() {
		return lastName;
	}

	public void setName(String lowerCaseName) {
		this.lastName = lowerCaseName;
	}
	
	
}
