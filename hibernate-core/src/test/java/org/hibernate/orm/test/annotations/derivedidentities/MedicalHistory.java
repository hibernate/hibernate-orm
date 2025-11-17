/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;

import jakarta.persistence.Entity;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class MedicalHistory {
	@Id
	@OneToOne
	@JoinColumn(name="FK")
	Person patient;

	@Lob
	byte[] xrayData;

	private MedicalHistory() {
	}

	public MedicalHistory(Person patient) {
		this.patient = patient;
	}

	public byte[] getXrayData() {
		return xrayData;
	}

	public void setXrayData(byte[] xrayData) {
		this.xrayData = xrayData;
	}
}
