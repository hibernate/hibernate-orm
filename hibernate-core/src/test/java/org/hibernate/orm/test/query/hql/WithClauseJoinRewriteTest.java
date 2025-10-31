/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementation of WithClauseTest.
 *
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
			WithClauseJoinRewriteTest.AbstractObject.class,
				WithClauseJoinRewriteTest.AbstractConfigurationObject.class,
				WithClauseJoinRewriteTest.ConfigurationObject.class
		}
)
@SessionFactory
public class WithClauseJoinRewriteTest {

	@Test
	@JiraKey(value = "HHH-11230")
	public void testInheritanceReAliasing(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {

					// Just assert that the query is successful
					List<Object[]> results = session.createQuery(
							"SELECT usedBy.id, usedBy.name, COUNT(inverse.id) " +
							"FROM " + AbstractConfigurationObject.class.getName() + " config " +
							"INNER JOIN config.usedBy usedBy " +
							"LEFT JOIN usedBy.uses inverse ON inverse.id = config.id " +
							"WHERE config.id = 0 " +
							"GROUP BY usedBy.id, usedBy.name",
							Object[].class
					).getResultList();
				}
		);
	}

	@Entity
	@Table( name = "config" )
	@Inheritance( strategy = InheritanceType.JOINED )
	public static abstract class AbstractConfigurationObject<T extends ConfigurationObject> extends AbstractObject {

		private String name;
		private Set<ConfigurationObject> uses = new HashSet<>();
		private Set<ConfigurationObject> usedBy = new HashSet<>();

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@ManyToMany( targetEntity = AbstractConfigurationObject.class, fetch = FetchType.LAZY, cascade = {} )
		public Set<ConfigurationObject> getUses () {
			return uses;
		}

		public void setUses(Set<ConfigurationObject> uses) {
			this.uses = uses;
		}

		@ManyToMany ( targetEntity = AbstractConfigurationObject.class, fetch = FetchType.LAZY, mappedBy = "uses", cascade = {} )
		public Set<ConfigurationObject> getUsedBy () {
			return usedBy;
		}

		public void setUsedBy(Set<ConfigurationObject> usedBy) {
			this.usedBy = usedBy;
		}
	}

	@Entity
	@Table( name = "config_config" )
	public static class ConfigurationObject extends AbstractConfigurationObject<ConfigurationObject> {

	}

	@MappedSuperclass
	public static class AbstractObject {

		private Long id;
		private Long version;

		@Id
		@GeneratedValue
		public Long getId () {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@Version
		@Column( nullable = false )
		public Long getVersion () {
			return version;
		}

		public void setVersion(Long version) {
			this.version = version;
		}
	}

}
