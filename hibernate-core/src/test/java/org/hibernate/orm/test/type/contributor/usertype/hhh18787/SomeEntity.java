/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.type.contributor.usertype.hhh18787;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Some entity, important is the property <code>customData</code>.
 */
@Entity
@Table(name = "whatever")
public class SomeEntity {
	@Id
	@GeneratedValue
	private Long id;

	@Column
	private CustomData[] customData;

	public SomeEntity() {
	}

	public SomeEntity(CustomData[] customData) {
		this.customData = customData;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public CustomData[] getCustomData() {
		return customData;
	}

	public void setCustomData(CustomData[] custom) {
		this.customData = custom;
	}
}
