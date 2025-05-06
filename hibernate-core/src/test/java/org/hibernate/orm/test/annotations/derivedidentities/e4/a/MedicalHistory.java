/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e4.a;
import java.io.Serializable;
import java.util.Date;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;


/**
 * @author Emmanuel Bernard
 */
@Entity
public class MedicalHistory implements Serializable {
	@Id
	@JoinColumn(name = "FK")
	@OneToOne
	Person patient;

	@Temporal(TemporalType.DATE)
	Date lastupdate;

	public MedicalHistory() {
	}

	public MedicalHistory(Person patient) {
		this.patient = patient;
	}
}
