/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.orphan.one2one.fk.reversed.bidirectional.multilevelcascade;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

@Entity
public class Tranchenmodell {


	private Long id;

	private List<Tranche> tranchen = new ArrayList<Tranche>();


	private Preisregelung preisregelung;


	@Id
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "tranchenmodell", fetch = FetchType.LAZY, orphanRemoval = true)
	public List<Tranche> getTranchen() {
		return tranchen;
	}

	public void setTranchen(List<Tranche> tranchen) {
		this.tranchen = tranchen;
	}

	@OneToOne(mappedBy="tranchenmodell", optional = true, fetch = FetchType.LAZY)
	public Preisregelung getPreisregelung() {
		return preisregelung;
	}

	public void setPreisregelung(Preisregelung preisregelung) {
		this.preisregelung = preisregelung;
	}


}
