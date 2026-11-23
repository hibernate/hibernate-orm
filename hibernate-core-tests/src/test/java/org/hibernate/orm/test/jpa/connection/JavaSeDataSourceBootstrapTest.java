package org.hibernate.orm.test.jpa.connection;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Persistence;
import jakarta.persistence.Table;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.internal.DataSourceConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.orm.test.util.connections.BaseDataSource;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/// Java SE JPA bootstrap accepts a DataSource instance in the integration map.
///
/// @author Steve Ebersole
@BaseUnitTest
@JiraKey("HHH-7779")
public class JavaSeDataSourceBootstrapTest {
	@ParameterizedTest
	@ValueSource(strings = { AvailableSettings.JAKARTA_NON_JTA_DATASOURCE, AvailableSettings.DATASOURCE })
	void testDataSourceInstance(String setting) throws Exception {
		DataSource dataSource = new BaseDataSource( Environment.getProperties() ) {
			@Override
			public Connection getConnection(String username, String password) {
				try {
					return getConnection();
				}
				catch (SQLException e) {
					throw new IllegalStateException( "Could not obtain test connection", e );
				}
			}
		};
		Thread thread = Thread.currentThread();
		ClassLoader original = thread.getContextClassLoader();
		URL descriptor = getClass().getClassLoader().getResource(
				"org/hibernate/orm/test/jpa/connection/java-se-datasource.xml"
		);
		assertNotNull( descriptor );
		ClassLoader loader = new ClassLoader( original ) {
			@Override
			public Enumeration<URL> getResources(String name) throws IOException {
				return name.equals( "META-INF/persistence.xml" )
						? Collections.enumeration( List.of( descriptor ) )
						: super.getResources( name );
			}
		};
		thread.setContextClassLoader( loader );
		try ( var factory = Persistence.createEntityManagerFactory( "java-se-datasource", Map.of(
				setting, dataSource,
				AvailableSettings.HBM2DDL_AUTO, "create-drop"
		) ) ) {
			var sessionFactory = factory.unwrap( SessionFactoryImplementor.class );
			var provider = sessionFactory.getServiceRegistry().getService( ConnectionProvider.class );
			assertSame( dataSource, assertInstanceOf( DataSourceConnectionProvider.class, provider ).getDataSource() );
			try ( var entityManager = factory.createEntityManager() ) {
				entityManager.getTransaction().begin();
				Item item = new Item();
				item.id = 1L;
				item.name = "from datasource";
				entityManager.persist( item );
				entityManager.getTransaction().commit();
			}
			try ( var entityManager = factory.createEntityManager() ) {
				assertEquals( "from datasource", entityManager.find( Item.class, 1L ).name );
			}
		}
		finally {
			thread.setContextClassLoader( original );
		}
	}

	@Entity(name = "JavaSeDataSourceItem")
	@Table(name = "java_se_datasource_item")
	public static class Item {
		@Id
		private Long id;
		private String name;
	}
}
