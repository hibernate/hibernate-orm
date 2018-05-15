/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.secondarytable;

import java.io.Serializable;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.Table;

import org.hibernate.Criteria;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.transaction.TransactionUtil;
import org.hibernate.test.schemaupdate.foreignkeys.definition.AbstractForeignKeyDefinitionTest;
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
	@org.hibernate.annotations.Table(appliesTo = "Cluster", optional = false)
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
