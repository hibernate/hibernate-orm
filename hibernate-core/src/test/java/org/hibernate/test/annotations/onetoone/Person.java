/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.onetoone;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

/**
 * @author Emmanuel Bernard
 * @author Gail Badner
 */
@Entity
public class Person {
	@Id @GeneratedValue(generator = "fk")
	@GenericGenerator(strategy = "foreign", name = "fk", parameters = @Parameter(name="property", value="personAddress"))
	private Integer id;

	@PrimaryKeyJoinColumn
	@OneToOne(optional=true)
	private PersonAddress personAddress;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public PersonAddress getPersonAddress() {
		return personAddress;
	}

	public void setPersonAddress(PersonAddress personAddress) {
		this.personAddress = personAddress;
	}
}
