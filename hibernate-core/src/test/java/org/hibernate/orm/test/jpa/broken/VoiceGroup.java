/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.broken;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Version;

import java.io.Serializable;
import java.util.Objects;

@NamedEntityGraph(name = "voiceGroup.graph", attributeNodes = { @NamedAttributeNode("primaryNumber")})
@Entity
public class VoiceGroup implements Serializable {

	private Integer id;
	private int version;
	private TelephoneNumber primaryNumber;

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

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "primaryNumber", nullable = true)
	public TelephoneNumber getPrimaryNumber() {
		return primaryNumber;
	}

	public void setPrimaryNumber(TelephoneNumber primaryNumber) {
		this.primaryNumber = primaryNumber;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof VoiceGroup voiceGroup) {
			return this == o || getId().equals(voiceGroup.getId());
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
