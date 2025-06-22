/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.override.mappedsuperclass;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Vlad Mihalcea
 */
@Entity
@AttributeOverride(name = "uid", column = @Column(name = "id_extend_table", insertable = false, updatable = false))
public class SubclassWithUuidAsId extends MappedSuperClassWithUuidAsBasic {

	@Id
	@Access(AccessType.PROPERTY)
	@Override
	public Long getUid() {
		return super.getUid();
	}
}
