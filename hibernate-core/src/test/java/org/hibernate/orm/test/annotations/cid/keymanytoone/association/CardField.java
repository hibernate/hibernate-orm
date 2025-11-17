/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cid.keymanytoone.association;

import java.io.Serializable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
@Entity
public class CardField implements Serializable {

	@EmbeddedId
	private PrimaryKey primaryKey;
	private String name;

	CardField(Card card, Key key) {
		this.primaryKey = new PrimaryKey( card, key);
	}

	CardField() {
	}

	public PrimaryKey getPrimaryKey() {
		return primaryKey;
	}

	public void setPrimaryKey(PrimaryKey primaryKey) {
		this.primaryKey = primaryKey;
	}
}
