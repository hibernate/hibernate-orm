/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.accesstype;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Mammals extends LivingBeing {
	private String id;
	private String nbrOfMammals;

	@Id
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getNbrOfMammals() {
		return nbrOfMammals;
	}

	public void setNbrOfMammals(String nbrOfMammals) {
		this.nbrOfMammals = nbrOfMammals;
	}
}
