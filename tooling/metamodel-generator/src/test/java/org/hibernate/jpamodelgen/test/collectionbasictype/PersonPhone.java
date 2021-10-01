package org.hibernate.jpamodelgen.test.collectionbasictype;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CustomType;
import org.hibernate.jpamodelgen.test.collectionbasictype.extras.Phone;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "Person")
public class PersonPhone {

	@Id
	private Long id;

	@Basic
	@CustomType( CommaDelimitedStringsType.class )
	private List<Phone> phones = new ArrayList<>();

	public List<Phone> getPhones() {
		return phones;
	}
}
