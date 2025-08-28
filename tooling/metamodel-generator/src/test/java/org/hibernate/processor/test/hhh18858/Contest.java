/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.hhh18858;

import jakarta.persistence.CascadeType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

import org.hibernate.annotations.ListIndexBase;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Contest {
	private int id;
	private Competitor[] results;
	private int[] heldIn;

	@Id
	@GeneratedValue
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@OneToMany(cascade = CascadeType.ALL)
	@OrderColumn(name = "pos")
	public Competitor[] getResults() {
		return results;
	}

	public void setResults(Competitor[] results) {
		this.results = results;
	}

	@ElementCollection
	@OrderColumn
	@ListIndexBase( 1 )
	public int[] getHeldIn() {
		return heldIn;
	}

	public void setHeldIn(int[] heldIn) {
		this.heldIn = heldIn;
	}

	/*public enum Month {
		January, February, March, April, May, June, July, August, September, October, November, December;
	};*/
}
