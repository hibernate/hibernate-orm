/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;


import java.io.Serializable;

import org.hibernate.annotations.LazyGroup;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity(name = "RoleEntity")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Table(name = "PP_DCRolleKey")
public class RoleEntity extends ModelEntity implements Serializable {

	@Basic
	@Column(name = "val")
	Short value;

	@ManyToOne(fetch = FetchType.LAZY)
	@LazyGroup("Key")
	@JoinColumn
	protected AbstractKey key = null;

	@ManyToOne(fetch = FetchType.LAZY)
	@LazyGroup("Key")
	@JoinColumn
	protected SpecializedKey specializedKey = null;

	public Short getValue() {
		return value;
	}

	public void setvalue(Short value) {
		this.value = value;
	}

	public AbstractKey getKey() {
		return key;
	}

	public void setKey(AbstractKey key) {
		this.key = key;
	}

	public SpecializedKey getSpecializedKey() {
		return specializedKey;
	}

	public void setSpecializedKey(SpecializedKey specializedKey) {
		this.specializedKey = specializedKey;
	}
}
