/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.processor.test.embeddable.genericsinheritance;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class ExampleEntity {
	private int id;

	private ExampleEmbedded<?> exampleEmbedded;

	@Id
	@GeneratedValue
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Embedded
	public ExampleEmbedded<?> getExampleEmbedded() {
		return exampleEmbedded;
	}
	public void setExampleEmbedded(ExampleEmbedded<?> exampleEmbedded) {
		this.exampleEmbedded = exampleEmbedded;
	}
}
