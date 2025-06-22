/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import java.sql.Statement;
import java.util.List;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.query.Query;
import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProvider;
import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProviderSettingProvider;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Jan Schatteman
 */
@DomainModel(annotatedClasses = { FetchSizeTest.MyEntity.class} )
@ServiceRegistry(
		settings = { @Setting(name = AvailableSettings.STATEMENT_FETCH_SIZE, value = "4") },
		settingProviders = {
				@SettingProvider(
						settingName = AvailableSettings.CONNECTION_PROVIDER,
						provider = PreparedStatementSpyConnectionProviderSettingProvider.class)
		}
)
@SessionFactory()
@RequiresDialect( value = H2Dialect.class )
@JiraKey(value = "HHH-16868")
public class FetchSizeTest {
	private PreparedStatementSpyConnectionProvider connectionProvider;

	@BeforeAll
	public void init(SessionFactoryScope scope) {
		final Map<String, Object> props = scope.getSessionFactory().getProperties();
		connectionProvider = (PreparedStatementSpyConnectionProvider) props.get( AvailableSettings.CONNECTION_PROVIDER );
	}

	@BeforeEach
	public void clear(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					for ( int i = 1; i <= 10; i++ ) {
						final MyEntity e = new MyEntity( i );
						session.persist( e );
					}
				}
		);
		connectionProvider.clear();
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void verifyJdbcFetchSizeIsSet(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Query<MyEntity> q = session.createQuery( "select e from MyEntity e", MyEntity.class );
					List<MyEntity> results = q.getResultList();
					try {
						List<Object[]> setJdbcFetchSizeCalls = connectionProvider.spyContext.getCalls(
								Statement.class.getMethod( "setFetchSize", int.class ),
								connectionProvider.getPreparedStatements().get( 0 )
						);
						assertEquals( 1, setJdbcFetchSizeCalls.size() );
						assertEquals( 4, setJdbcFetchSizeCalls.get( 0 )[0] );
					}
					catch (NoSuchMethodException e) {
						fail(e);
					}
					assertEquals(10, results.size());
				}
		);
	}
	@Entity(name = "MyEntity")
	@Table(name = "MyEntity")
	public static class MyEntity {
		@Id
		Integer id;

		public MyEntity() {
		}

		public MyEntity(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

}
