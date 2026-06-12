/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind.id;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.TenantId;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * @author Steve Ebersole
 */
@Entity
@Table(name = "agg_id_entities")
public class AggregatedIdEntity {
	@EmbeddedId
	private Pk id;

	@Embedded
	@NaturalId
	private NatKey naturalId;

	@Version
	private Integer version;

	@TenantId
	private String tenantId;

	@Embeddable
	public static class Pk {
		private Integer id1;
		private Integer id2;
	}

	@Embeddable
	public static class NatKey {
		private Integer key1;
		private Integer key2;
	}
}
