/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.converted.converter;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Jan Schatteman
 */
@Entity(name = "EntityWithEnum")
public class EntityWithEnum {
	@Id
	private Long id;
	@Convert(converter = AttributeConverterAndFunctionTest.StateJpaConverter.class)
	private AttributeConverterAndFunctionTest.State state;

	public EntityWithEnum() {
	}

	public EntityWithEnum(Long id, AttributeConverterAndFunctionTest.State state) {
		this.id = id;
		this.state = state;
	}
}
