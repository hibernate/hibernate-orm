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
