/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.orphan.one2one.fk.bidirectional.multilevelcascade;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Tranchenmodell {

	@Id
	@GeneratedValue
	private Long id;

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "tranchenmodell", fetch = FetchType.LAZY, orphanRemoval = true)
	@OrderColumn
	private List<Tranche> tranchen = new ArrayList<Tranche>();

	@OneToOne(optional = true, fetch = FetchType.LAZY)
	private Preisregelung preisregelung;

	@OneToOne(mappedBy = "tranchenmodell", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private X x;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public List<Tranche> getTranchen() {
		return tranchen;
	}

	public Preisregelung getPreisregelung() {
		return preisregelung;
	}

	public void setPreisregelung(Preisregelung preisregelung) {
		this.preisregelung = preisregelung;
	}

	public X getX() {
		return x;
	}

	public void setX(X x) {
		this.x = x;
	}
}
