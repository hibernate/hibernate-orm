/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.cut.generic;

import org.hibernate.annotations.CompositeType;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class GenericCompositeUserTypeEntity {
	@Embedded
	@CompositeType(value = EnumPlaceholderUserType.class)
	@AttributeOverrides({
			@AttributeOverride(name = "type", column = @Column(name = "TYPE", updatable = false)),
			@AttributeOverride(name = "jsonValue", column = @Column(name = "DATA", updatable = false, columnDefinition = "clob"))
	})
	protected EnumPlaceholder placeholder;
	@Id
	private final Long id;

	public GenericCompositeUserTypeEntity(EnumPlaceholder placeholder) {
		this.id = System.currentTimeMillis();
		this.placeholder = placeholder;
	}
}
