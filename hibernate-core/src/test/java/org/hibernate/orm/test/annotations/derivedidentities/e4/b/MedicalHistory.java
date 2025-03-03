/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e4.b;
import java.util.Date;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class MedicalHistory {

	@Id String id; // overriding not allowed ... // default join column name is overridden @MapsId

	@MapsId
	@JoinColumn(name = "FK")
	@OneToOne(cascade= CascadeType.PERSIST)
	Person patient;

	@Temporal(TemporalType.DATE)
	Date lastupdate;

	public MedicalHistory() {
	}

	public MedicalHistory(String id, Person patient) {
		this.id = id;
		this.patient = patient;
	}
}
