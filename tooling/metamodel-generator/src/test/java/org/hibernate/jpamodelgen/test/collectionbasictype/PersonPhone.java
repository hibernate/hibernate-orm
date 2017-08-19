package org.hibernate.jpamodelgen.test.collectionbasictype;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.jpamodelgen.test.collectionbasictype.extras.Phone;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "Person")
@TypeDef( name = "comma_delimited_strings", typeClass = CommaDelimitedStringsType.class)
public class PersonPhone {

	@Id
	private Long id;

	@Type(type = "comma_delimited_strings")
	private List<Phone> phones = new ArrayList<>();

	public List<Phone> getPhones() {
		return phones;
	}
}
