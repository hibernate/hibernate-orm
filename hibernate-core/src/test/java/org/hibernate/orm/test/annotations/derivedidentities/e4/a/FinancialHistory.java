/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e4.a;
import java.io.Serializable;
import java.util.Date;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;


/**
 * @author Emmanuel Bernard
 */
@Entity
public class FinancialHistory implements Serializable {
	@Id
	//@JoinColumn(name = "FK")
	@ManyToOne
	Person patient;

	@Temporal(TemporalType.DATE)
	Date lastUpdate;

	public FinancialHistory() {
	}

	public FinancialHistory(Person patient) {
		this.patient = patient;
	}
}
