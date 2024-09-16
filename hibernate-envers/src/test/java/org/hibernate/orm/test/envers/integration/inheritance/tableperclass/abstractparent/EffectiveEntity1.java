/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.inheritance.tableperclass.abstractparent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.hibernate.envers.Audited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Table(name = "ENTITY_1")
@Audited
public class EffectiveEntity1 extends AbstractEntity {
	@Column
	public String specificField1;

	public EffectiveEntity1() {
	}

	public EffectiveEntity1(Long id, String commonField, String specificField1) {
		super( id, commonField );
		this.specificField1 = specificField1;
	}
}
