/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.collection;

import java.util.EnumMap;
import java.util.Map;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Entity
@Audited
public class EnumMapEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@OneToMany(cascade = CascadeType.ALL)
	private Map<EnumType, EnumMapType> types = new EnumMap<>(EnumType.class);

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Map<EnumType, EnumMapType> getTypes() {
		return types;
	}

	public void setTypes(Map<EnumType, EnumMapType> types) {
		this.types = types;
	}

	public enum EnumType {
		TYPE_A,
		TYPE_B,
		TYPE_C
	}
}
