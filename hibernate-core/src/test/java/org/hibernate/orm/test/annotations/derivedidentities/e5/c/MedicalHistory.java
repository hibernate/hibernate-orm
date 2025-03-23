/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e5.c;
import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;


/**
 * @author Emmanuel Bernard
 */
@Entity
public class MedicalHistory implements Serializable {
	@Id
	Integer id;

	@MapsId
	@JoinColumn(name = "patient_id")
	@OneToOne
	Person patient;
}
