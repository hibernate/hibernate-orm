/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.metamodel.generics.embeddable;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.hibernate.orm.test.metamodel.generics.embeddable.AbstractValueObject.VALUE;

@Entity
public class Parent {
	@Id
	@GeneratedValue
	private Long id;

	@Embedded
	@AttributeOverride( name = VALUE, column = @Column( name = "some_string" ) )
	private SomeString someString;

	@Embedded
	@AttributeOverride( name = VALUE, column = @Column( name = "some_date" ) )
	private CreationDate date;

	@Embedded
	@AttributeOverride( name = VALUE, column = @Column( name = "some_number" ) )
	private SomeNumber someNumber;

	public Parent() {
	}

	public Parent(final SomeString someString, final CreationDate date, final SomeNumber someNumber) {
		this.someString = someString;
		this.date = date;
		this.someNumber = someNumber;
	}
}
