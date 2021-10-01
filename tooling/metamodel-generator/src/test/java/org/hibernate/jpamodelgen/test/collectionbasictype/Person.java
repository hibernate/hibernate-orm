package org.hibernate.jpamodelgen.test.collectionbasictype;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CustomType;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "Person")
public class Person {

	@Id
	private Long id;

	@Basic
	@CustomType( CommaDelimitedStringsType.class )
	private List<String> phones = new ArrayList<>();

	public List<String> getPhones() {
		return phones;
	}
}
