/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetoone.hhh4851;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;

@Entity
@DiscriminatorValue(value = "T")
public class Device extends Hardware {

	private ManagedDevice managedDevice;
	private String tag;

	public Device() {
	}

	@OneToOne(fetch = FetchType.LAZY, mappedBy = "device")
	public ManagedDevice getManagedDevice() {
		return managedDevice;
	}

	@Column(unique = true, nullable = true)
	public String getTag() {
		return tag;
	}

	public void setManagedDevice(ManagedDevice logicalterminal) {
		this.managedDevice = logicalterminal;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

}
