/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.inheritance.joined.emptychild;

import jakarta.persistence.Entity;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Audited
public class EmptyChildEntity extends ParentEntity {
	public EmptyChildEntity() {
	}

	public EmptyChildEntity(Integer id, String data) {
		super( id, data );
	}
}
