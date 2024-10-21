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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;

import java.io.Serializable;
import java.util.Objects;

@Entity
public class TelephoneNumber implements Serializable {

	private Integer id;
	private int version;
	private String number;
	private VoiceGroup voiceGroup;
	private Provider provider;

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

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "voiceGroup", nullable = false)
	public VoiceGroup getVoiceGroup() {
		return voiceGroup;
	}

	public void setVoiceGroup(VoiceGroup voiceGroup) {
		this.voiceGroup = voiceGroup;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "provider", nullable = false)
	public Provider getProvider() {
		return provider;
	}

	public void setProvider(Provider provider) {
		this.provider = provider;
	}

	@Override
	public boolean equals(Object o) {
		if ( o instanceof TelephoneNumber telephoneNumber ) {
			return this == o || getId().equals( telephoneNumber.getId() );
		}
		else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hashCode( id );
	}
}
