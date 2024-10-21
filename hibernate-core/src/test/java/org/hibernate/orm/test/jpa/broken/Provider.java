/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.broken;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import java.io.Serializable;
import java.util.Objects;

@Entity
public class Provider implements Serializable {

	private Integer id;
	private int version;
	private ThirdParty thirdParty;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Version
	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	@OneToOne(mappedBy = "provider", optional = false)
	public ThirdParty getThirdParty() {
		return thirdParty;
	}

	public void setThirdParty(ThirdParty thirdParty) {
		this.thirdParty = thirdParty;
	}

	@Transient
	public String getName() {
		return thirdParty.getName();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Provider provider) {
			return this == o || getId().equals(provider.getId());
		}
		else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}
