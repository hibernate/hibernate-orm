package org.hibernate.test.annotations.entity;

import javax.persistence.MappedSuperclass;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Parameter;


@TypeDef(
		name = "lowerCase",
		typeClass = CasterStringType.class,
		parameters = {
			@Parameter(name = "cast", value = "lower")
		}
)

@MappedSuperclass
public class FirstName {

	@Type(type="lowerCase")
	private String firstName;

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String lowerCaseName) {
		this.firstName = lowerCaseName;
	}
	
	
}
