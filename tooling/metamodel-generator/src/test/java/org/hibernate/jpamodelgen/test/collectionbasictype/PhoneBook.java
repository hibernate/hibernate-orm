package org.hibernate.jpamodelgen.test.collectionbasictype;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.annotations.CustomType;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "Person")
public class PhoneBook {

	@Id
	private Long id;

	@Basic
	@CustomType( CommaDelimitedStringsMapType.class )
	private Map<String, String> phones = new HashMap<>();

	public Map<String, String> getPhones() {
		return phones;
	}
}
