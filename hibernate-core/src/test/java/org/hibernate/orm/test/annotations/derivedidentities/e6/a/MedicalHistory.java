/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e6.a;
import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
@IdClass(PersonId.class)
public class MedicalHistory implements Serializable {
	@Id
	@JoinColumn(name = "FK1", referencedColumnName = "firstName")
	@JoinColumn(name = "FK2", referencedColumnName = "lastName")
	@OneToOne
	Person patient;

	public void setPatient(Person patient) {
		this.patient = patient;
	}
}
