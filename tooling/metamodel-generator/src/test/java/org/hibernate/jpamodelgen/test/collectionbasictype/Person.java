package org.hibernate.jpamodelgen.test.collectionbasictype;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "Person")
@TypeDef( name = "comma_delimited_strings", typeClass = CommaDelimitedStringsType.class)
public class Person {

	@Id
	private Long id;

	@Type(type = "comma_delimited_strings")
	private List<String> phones = new ArrayList<>();

	public List<String> getPhones() {
		return phones;
	}
}
