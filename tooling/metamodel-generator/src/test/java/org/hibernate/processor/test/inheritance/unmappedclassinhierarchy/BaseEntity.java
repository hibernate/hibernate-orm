/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.inheritance.unmappedclassinhierarchy;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

@Entity
@Access(AccessType.FIELD)
public class BaseEntity {
	@Id
	@SequenceGenerator(name = "test1_id_gen", sequenceName = "test1_seq")
	@GeneratedValue(generator = "test1_id_gen", strategy = GenerationType.SEQUENCE)
	protected Integer id;

	protected String name;

	public BaseEntity() {
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}
}
