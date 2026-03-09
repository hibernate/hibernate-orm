/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.stateless;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.envers.Audited;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;

@DomainModel(annotatedClasses = {
		// mappings
		CollectionStatelessSessionTests.EntityA.class, CollectionStatelessSessionTests.EntityB.class, CollectionStatelessSessionTests.EntityC.class},
		xmlMappings = {})
@ServiceRegistry(settings = {
		// For your own convenience to see generated queries:
		@Setting(name = AvailableSettings.SHOW_SQL, value = "true"),
		@Setting(name = AvailableSettings.FORMAT_SQL, value = "true")})
@SessionFactory
public class CollectionStatelessSessionTests {

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-19818")
	void statelessInsert(SessionFactoryScope scope) throws Exception {
		scope.inStatelessTransaction( action -> {
			action.insert( new EntityA() );
		} );
	}

	@Entity(name = "ENTITY_A")
	@Table(name = "ENTITY_A")
	public static class EntityA {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		@Column(name = "ID")
		Integer id;

		@OneToMany
		@JoinColumn(name = "ENTITY_A")
		Collection<EntityB> children = new ArrayList<>();
	}

	@Entity(name = "ENTITY_B")
	@Table(name = "ENTITY_B")
	public static class EntityB {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		@Column(name = "ID")
		Integer id;
	}

	@Entity(name = "ENTITY_C")
	@Audited
	@Table(name = "ENTITY_C")
	public static class EntityC {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		@Column(name = "ID")
		Integer id;
	}
}
