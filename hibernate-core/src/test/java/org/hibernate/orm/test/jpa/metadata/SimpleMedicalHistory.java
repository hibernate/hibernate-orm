/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metadata;
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
public class SimpleMedicalHistory implements Serializable {

	@Temporal(TemporalType.DATE)
	Date lastupdate;

	@Id
	@JoinColumn(name = "FK")
	@OneToOne
	SimplePerson patient;
}
