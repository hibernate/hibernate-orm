/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.id.entities;
import java.io.Serializable;
import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * Blown precision on related entity when &#064;JoinColumn is used.
 * Does not cause an issue on HyperSonic, but replicates nicely on PGSQL.
 *
 * @see ANN-748
 * @author Andrew C. Oliver andyspam@osintegrators.com
 */
@Entity
public class PointyTooth implements Serializable {
	@Id
	@Column(name = "id", precision = 128, scale = 0)
	private BigDecimal id;

	@ManyToOne
	@JoinColumn(name = "bunny_id")
	Bunny bunny;

	public void setBunny(Bunny bunny) {
		this.bunny = bunny;
	}

	public BigDecimal getId() {
		return id;
	}
}
