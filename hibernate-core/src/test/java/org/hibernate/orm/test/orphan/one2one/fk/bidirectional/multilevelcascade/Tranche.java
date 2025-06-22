/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.orphan.one2one.fk.bidirectional.multilevelcascade;

import jakarta.persistence.*;

@Entity
public class Tranche {

	@Id
	@GeneratedValue
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.EAGER)
	private Tranchenmodell tranchenmodell;

	@OneToOne(mappedBy = "tranche", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private Y y;

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

	public Y getY() {
		return y;
	}

	public void setY(Y y) {
		this.y = y;
	}
}
