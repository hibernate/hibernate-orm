/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import java.io.Serializable;

import org.hibernate.annotations.LazyGroup;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;


@Entity(name="SpecializedEntity")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Table(name="PP_PartnerZusatzKuerzelZR")
public class SpecializedEntity implements Serializable {

	@Id
	Long id;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name="TheValue")
	String value;

	@ManyToOne(fetch=FetchType.LAZY)
	@LazyGroup("SpecializedKey")
	@JoinColumn
	protected SpecializedKey specializedKey = null;

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public SpecializedKey getSpecializedKey() {
		return specializedKey;
	}

	public void setSpecializedKey(SpecializedKey specializedKey) {
			this.specializedKey = specializedKey;
	}
}
