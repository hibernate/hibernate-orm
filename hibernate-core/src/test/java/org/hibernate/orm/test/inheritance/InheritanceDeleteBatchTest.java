/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.query.sqm.mutation.internal.inline.InlineMutationStrategy;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Query;
import jakarta.persistence.Table;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-13214")
@DomainModel(
		annotatedClasses = {
				InheritanceDeleteBatchTest.TestEntity.class,
				InheritanceDeleteBatchTest.TestEntityType1.class,
				InheritanceDeleteBatchTest.TestEntityType2.class
		}
)
@SessionFactory(
		generateStatistics = true
)
@ServiceRegistry(
		settingProviders = @SettingProvider(
				settingName = AvailableSettings.QUERY_MULTI_TABLE_MUTATION_STRATEGY,
				provider = InheritanceDeleteBatchTest.TableMutationStrategyProvider.class
		)
)
public class InheritanceDeleteBatchTest {

	public static class TableMutationStrategyProvider implements SettingProvider.Provider<String> {
		@Override
		public String getSetting() {
			return InlineMutationStrategy.class.getName();
		}
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new TestEntity( 1 ) );
			session.persist( new TestEntityType1( 2 ) );
			session.persist( new TestEntityType2( 3 ) );
			session.persist( new TestEntityType2( 4 ) );
		} );
	}

	@Test
	public void testDelete(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction( session -> {
			for ( int i = 1; i <= 4; i++ ) {
				Query deleteQuery = session.createQuery( "delete TestEntity e where e.id = :id" );
				deleteQuery.setParameter( "id", i );
				deleteQuery.executeUpdate();
				assertThat( statistics.getPrepareStatementCount(), is( 4L ) );
				statistics.clear();
			}
		} );
	}

	@Entity(name = "TestEntity")
	@Inheritance(strategy = InheritanceType.JOINED)
	@Table(name = "test_entity")
	public static class TestEntity {
		@Id
		int id;

		private String field;

		public TestEntity() {
		}

		public TestEntity(int id) {
			this.id = id;
		}
	}

	@Entity(name = "TestEntityType1")
	@Table(name = "test_entity_type1")
	public static class TestEntityType1 extends TestEntity {

		public TestEntityType1(int id) {
			super( id );
		}
	}

	@Entity(name = "TestEntityType2")
	@Table(name = "test_entity_type2")
	public static class TestEntityType2 extends TestEntity {
		public TestEntityType2(int id) {
			super( id );
		}
	}
}
