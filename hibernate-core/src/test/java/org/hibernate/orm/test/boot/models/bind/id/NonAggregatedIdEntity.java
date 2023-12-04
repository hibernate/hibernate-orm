/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.bind.id;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.TenantId;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * @author Steve Ebersole
 */
@Entity
@Table(name = "non_agg_id_entities")
@IdClass( NonAggregatedIdEntity.Pk.class )
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
public class NonAggregatedIdEntity {
	@Id private Integer id1;
	@Id private Integer id2;
	@Version private Integer version;
	@TenantId private String tenantId;
	@NaturalId private Integer naturalKey1;
	@NaturalId private Integer naturalKey2;

	public static class Pk {
		private Integer id1;
		private Integer id2;
	}
}
