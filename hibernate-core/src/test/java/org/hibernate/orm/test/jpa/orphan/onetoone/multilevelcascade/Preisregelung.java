/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.orphan.onetoone.multilevelcascade;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

@Entity
public class Preisregelung {
	@Id
	@GeneratedValue
	private Long id;

	@OneToOne(mappedBy = "preisregelung", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private Tranchenmodell tranchenmodell;


	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Tranchenmodell getTranchenmodell() {
		return tranchenmodell;
	}

	public void setTranchenmodell(Tranchenmodell tranchenmodell) {
		this.tranchenmodell = tranchenmodell;
	}
}
