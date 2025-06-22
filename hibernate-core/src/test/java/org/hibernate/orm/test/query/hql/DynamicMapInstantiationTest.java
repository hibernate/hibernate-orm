/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.logger.Triggerable;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.logger.LoggerInspectionExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.jboss.logging.Logger;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Query;
import jakarta.persistence.Table;

import java.lang.invoke.MethodHandles;

@DomainModel(
		annotatedClasses = {
				DynamicMapInstantiationTest.Parent.class
		}
)
@SessionFactory
@JiraKey( value = "HHH-16061")
public class DynamicMapInstantiationTest {

	private Triggerable triggerable;

	@RegisterExtension
	public LoggerInspectionExtension logger = LoggerInspectionExtension
			.builder().setLogger(
					Logger.getMessageLogger( MethodHandles.lookup(), CoreMessageLogger.class, SqmDynamicInstantiation.class.getName() )
			).build();

	@BeforeEach
	public void setUp() {
		triggerable = logger.watchForLogMessages( "Argument [org.hibernate.orm.test.query.hql.DynamicMapInstantiationTest" );
		triggerable.reset();
	}

	@Test
	public void testSimpleDynamicMapInstantiationWithoutAlias(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Query cb = session.createQuery( "select new map(p) from Parent p" );

					cb.getResultList();
				}
		);
		Assertions.assertTrue(
				triggerable.wasTriggered(),
				"A warning message must be thrown when the Dynamic map declare an injection alias "
		);
		triggerable.reset();
	}

	@Test
	public void testSimpleDynamicMapInstantiationWithAlias(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Query cb = session.createQuery( "select new map(p as parent) from Parent p" );

					cb.getResultList();
				}
		);
		Assertions.assertFalse(
				triggerable.wasTriggered(),
				"A warning message must not be thrown when the Dynamic map declare an injection alias"
		);
		triggerable.reset();
	}

	@Entity(name = "Parent")
	@Table(name = "parent_table")
	public class Parent {
		private Long id;

		private String name;

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		public Long getId() {
			return this.id;
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
	}
}
