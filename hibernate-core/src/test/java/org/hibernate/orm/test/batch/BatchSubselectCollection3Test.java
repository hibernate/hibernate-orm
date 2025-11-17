/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@DomainModel(
		annotatedClasses = {
				BatchSubselectCollection3Test.SimpleA.class,
				BatchSubselectCollection3Test.SimpleB.class,
				BatchSubselectCollection3Test.SimpleC.class,
				BatchSubselectCollection3Test.SimpleD.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, value = "10"),
				@Setting(name = AvailableSettings.FORMAT_SQL, value = "false"),
		}
)
@JiraKey("HHH-17670")
public class BatchSubselectCollection3Test {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist( new SimpleA( 1L ) );
					session.persist( new SimpleA( 2L ) );

					session.persist( new SimpleC( 1L ) );
					session.persist( new SimpleC( 2L ) );
				}
		);
	}

	@Test
	public void testQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// To trigger the NPE, it is vital to have a OneToManyTableGroup before the table group that is selected
					session.createQuery( "select obj2 from SimpleA obj left outer join obj.children obj1, SimpleC obj2" )
							.getResultList();
				}
		);
	}

	@Entity(name = "SimpleA")
	public static class SimpleA {
		@Id
		private Long id;
		@OneToMany(mappedBy = "parent")
		private List<SimpleB> children = new ArrayList<>();

		public SimpleA() {
		}

		public SimpleA(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "SimpleB")
	public static class SimpleB {
		@Id
		private Long id;
		@ManyToOne
		private SimpleA parent;
	}

	@Entity(name = "SimpleC")
	public static class SimpleC {
		@Id
		private Long id;
		private String s;
		@OneToMany
		@Fetch(FetchMode.SUBSELECT)
		private List<SimpleD> children2 = new ArrayList<>();

		public SimpleC() {
		}

		public SimpleC(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "SimpleD")
	public static class SimpleD {
		@Id
		private Long id;
		private String s;
	}
}
