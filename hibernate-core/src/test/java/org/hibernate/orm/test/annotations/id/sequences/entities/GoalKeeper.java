/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.id.sequences.entities;
import jakarta.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class GoalKeeper extends Footballer {
	public GoalKeeper() {
	}

	public GoalKeeper(String firstname, String lastname, String club) {
		super( firstname, lastname, club );
	}
}
