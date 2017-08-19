package org.hibernate.jpamodelgen.test.collectionbasictype;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "Person")
@TypeDef( name = "comma_delimited_string_map", typeClass = CommaDelimitedStringsMapType.class)
public class PhoneBook {

	@Id
	private Long id;

	@Type(type = "comma_delimited_string_map")
	private Map<String, String> phones = new HashMap<>();

	public Map<String, String> getPhones() {
		return phones;
	}
}
