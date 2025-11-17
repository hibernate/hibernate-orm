/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.idClass;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

@Entity
@IdClass( MyEntityId.class )
public class MyEntity {

	@Id
	private Long idA;

	@Id
	private Long idB;

	private String notes;

	public MyEntityId getId() {
		return new MyEntityId( idA, idB );
	}

	public void setId(MyEntityId id) {
		this.idA = id.getIdA();
		this.idB = id.getIdB();
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}
}
