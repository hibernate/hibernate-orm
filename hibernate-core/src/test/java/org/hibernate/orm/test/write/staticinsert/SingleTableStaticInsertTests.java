/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.write.staticinsert;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
public class SingleTableStaticInsertTests {
	@Test
	@DomainAndFactory
	@ServiceRegistry(
			settings = {
					@Setting(name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
					@Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "-1")
			}
	)
	public void unBatchedSingleTableTest(SessionFactoryScope scope) {
		verify( scope, 3 );
	}

	@Test
	@DomainAndFactory
	@ServiceRegistry(
			settings = {
					@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
					@Setting( name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "5" )
			}
	)
	public void batchedSingleTableTest(SessionFactoryScope scope) {
		verify( scope, 1 );
	}


	public void verify(SessionFactoryScope scope, int expectedPrepCount) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction( (session) -> {
			session.persist( new SimpleEntity( 1, "First" ) );
			session.persist( new SimpleEntity( 2, "Second" ) );
			session.persist( new SimpleEntity( 3, "Third" ) );
		} );

		assertThat( statementInspector.getSqlQueries() ).hasSize( expectedPrepCount );
		assertThat( statistics.getPrepareStatementCount() ).isEqualTo( expectedPrepCount );

		scope.inTransaction( (session) -> {
			final Long count = session
					.createSelectionQuery( "select count(1) from SimpleEntity", Long.class )
					.getSingleResult();
			assertThat( count ).isEqualTo( 3L );
		} );
	}

	@Entity( name = "SimpleEntity" )
	@Table( name = "SimpleEntity" )
	public static class SimpleEntity {
		@Id
		private Integer id;
		@Basic
		private String name;

		private SimpleEntity() {
			// for use by Hibernate
		}

		public SimpleEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Target({ ElementType.TYPE, ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	@DomainModel( annotatedClasses = SimpleEntity.class )
	@SessionFactory( useCollectingStatementInspector = true )
	@interface DomainAndFactory {}
}
