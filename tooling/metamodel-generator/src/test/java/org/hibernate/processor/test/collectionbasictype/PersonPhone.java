/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.collectionbasictype;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Type;
import org.hibernate.processor.test.collectionbasictype.extras.Phone;

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
	@Type( CommaDelimitedStringsType.class )
	private List<Phone> phones = new ArrayList<>();

	public List<Phone> getPhones() {
		return phones;
	}
}
