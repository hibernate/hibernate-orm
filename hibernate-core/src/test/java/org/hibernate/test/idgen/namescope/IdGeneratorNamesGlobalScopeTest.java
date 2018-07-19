/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.idgen.namescope;

import java.util.Properties;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.TableGenerator;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.internal.util.config.ConfigurationHelper;

import org.junit.Test;

/**
 * @author Andrea Boriero
 */
public class IdGeneratorNamesGlobalScopeTest {

	@Test(expected = IllegalArgumentException.class)
	public void testNoSequenceGenratorNameClash() {
		buildSessionFactory();
	}

	@Entity(name = "FirstEntity")
	@TableGenerator(
			name = "table-generator",
			table = "table_identifier_2",
			pkColumnName = "identifier",
			valueColumnName = "value",
			allocationSize = 5
	)
	public static class FirstEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.TABLE, generator = "table-generator")
		private Long id;
	}

	@Entity(name = "SecondEntity")
	@TableGenerator(
			name = "table-generator",
			table = "table_identifier",
			pkColumnName = "identifier",
			valueColumnName = "value",
			allocationSize = 5
	)
	public static class SecondEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.TABLE, generator = "table-generator")
		private Long id;
	}

	private void buildSessionFactory() {
		Configuration configuration =  new Configuration();
		final BootstrapServiceRegistryBuilder builder = new BootstrapServiceRegistryBuilder();
		builder.applyClassLoader( getClass().getClassLoader() );
		BootstrapServiceRegistry bootRegistry = builder.build();
		StandardServiceRegistryImpl serviceRegistry = buildServiceRegistry( bootRegistry, configuration );
		configuration.addAnnotatedClass( FirstEntity.class );
		configuration.addAnnotatedClass( SecondEntity.class );
		configuration.buildSessionFactory( serviceRegistry );
	}

	protected StandardServiceRegistryImpl buildServiceRegistry(BootstrapServiceRegistry bootRegistry, Configuration configuration) {
		Properties properties = new Properties();
		properties.putAll( configuration.getProperties() );
		properties.put( AvailableSettings.JPA_ID_GENERATOR_GLOBAL_SCOPE_COMPLIANCE, "true" );
		ConfigurationHelper.resolvePlaceHolders( properties );

		StandardServiceRegistryBuilder cfgRegistryBuilder = configuration.getStandardServiceRegistryBuilder();

		StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder( bootRegistry, cfgRegistryBuilder.getAggregatedCfgXml() )
				.applySettings( properties );

		return (StandardServiceRegistryImpl) registryBuilder.build();
	}
}
