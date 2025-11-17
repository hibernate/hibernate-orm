/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.orphan.one2one.fk.bidirectional.multilevelcascade;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

/**
 * @author Gail Badner
 */
@Entity
public class Y {

	@Id
	@GeneratedValue
	private Long id;

	@OneToOne(optional = true, fetch = FetchType.LAZY)
	private Tranche tranche;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Tranche getTranche() {
		return tranche;
	}

	public void setTranche(Tranche tranche) {
		this.tranche = tranche;
	}
}
