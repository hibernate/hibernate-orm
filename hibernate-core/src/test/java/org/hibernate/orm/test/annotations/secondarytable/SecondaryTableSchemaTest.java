/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.secondarytable;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.annotations.SecondaryRow;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(value = H2Dialect.class)
public class SecondaryTableSchemaTest
		extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Cluster.class,
		};
	}

	protected void addConfigOptions(Map options) {
		options.put(
			AvailableSettings.URL,
			options.get( AvailableSettings.URL ) + ";INIT=CREATE SCHEMA IF NOT EXISTS schema1\\;CREATE SCHEMA IF NOT EXISTS schema2;"
		);
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			List<Cluster> clusters = entityManager.createQuery( "select c from Cluster c" ).getResultList();

			assertTrue(clusters.isEmpty());
		} );
	}

	@Entity(name = "Cluster")
	@Table(name = "cluster", schema = "schema1")
	@SecondaryTable(name = "Cluster", schema="schema2", pkJoinColumns = { @PrimaryKeyJoinColumn(name = "clusterid") })
	@SecondaryRow(table = "Cluster", optional = false)
	@OptimisticLocking(type = OptimisticLockType.DIRTY)
	@DynamicUpdate
	public static class Cluster implements Serializable {
		private static final long serialVersionUID = 3965099001305947412L;

		@Id
		@Column(name = "objid")
		private Long id;

		private String uuid;

		private String resourceKey;

		private String name;

		@Column(name = "lastSync", table = "Cluster")
		private Long lastSync;

		@Column(name = "healthStatus", table = "Cluster")
		private Integer healthStatus;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getResourceKey() {
			return resourceKey;
		}

		public void setResourceKey(String resourceKey) {
			this.resourceKey = resourceKey;
		}

		public String getUuid() {
			return uuid;
		}

		public void setUuid(String uuid) {
			this.uuid = uuid;
		}

		public Long getLastSync() {
			return lastSync;
		}

		public void setLastSync(Long lastSync) {
			this.lastSync = lastSync;
		}

		public Integer getHealthStatus() {
			return healthStatus;
		}

		public void setHealthStatus(Integer healthStatus) {
			this.healthStatus = healthStatus;
		}

	}
}
