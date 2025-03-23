/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Entity
@Audited
public class EnumEntity {
	@Id
	private Integer id;

	@Enumerated(EnumType.STRING)
	@Column(name = "client_option")
	@Audited(modifiedColumnName = "client_option_mod")
	private EnumOption option;

	EnumEntity() {

	}

	public EnumEntity(Integer id, EnumOption option) {
		this.id = id;
		this.option = option;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public EnumOption getOption() {
		return option;
	}

	public void setOption(EnumOption option) {
		this.option = option;
	}
}
