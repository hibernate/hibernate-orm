/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind.id;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.TenantId;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

/**
 * @author Steve Ebersole
 */
@Entity
public class BasicIdEntity {
	@Id
	private Integer id;

	@Version
	private Integer version;

	@TenantId
	private String tenantId;

	@NaturalId
	private String naturalId;
}
