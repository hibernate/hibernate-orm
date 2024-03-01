package org.hibernate.processor.test.collectionbasictype;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Type;

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
	@Type( CommaDelimitedStringsType.class )
	private List<String> phones = new ArrayList<>();

	public List<String> getPhones() {
		return phones;
	}
}
