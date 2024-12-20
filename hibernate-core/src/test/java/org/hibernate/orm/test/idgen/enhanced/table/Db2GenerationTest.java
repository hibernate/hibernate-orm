/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.enhanced.table;

import java.util.Properties;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Table;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class Db2GenerationTest {
	@Test
	@JiraKey( value = "HHH-9850" )
	public void testNewGeneratorTableCreationOnDb2() {
		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, DB2Dialect.class.getName() )
				.build();

		try {
			final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr ).buildMetadata();

			assertEquals( 0, metadata.getDatabase().getDefaultNamespace().getTables().size() );

			final TableGenerator generator = new TableGenerator();

			generator.configure(
					new GeneratorCreationContext() {
						@Override
						public Database getDatabase() {
							return metadata.getDatabase();
						}

						@Override
						public ServiceRegistry getServiceRegistry() {
							return ssr;
						}

						@Override
						public String getDefaultCatalog() {
							return "";
						}

						@Override
						public String getDefaultSchema() {
							return "";
						}

						@Override
						public PersistentClass getPersistentClass() {
							return null;
						}

						@Override
						public RootClass getRootClass() {
							return null;
						}

						@Override
						public Property getProperty() {
							return null;
						}

						@Override
						public Type getType() {
							return metadata.getDatabase()
									.getTypeConfiguration()
									.getBasicTypeRegistry()
									.resolve( StandardBasicTypes.INTEGER );
						}
					},
					new Properties()
			);

			generator.registerExportables( metadata.getDatabase() );

			assertEquals( 1, metadata.getDatabase().getDefaultNamespace().getTables().size() );

			Database database = metadata.getDatabase();
			final Table table = database.getDefaultNamespace().getTables().iterator().next();
			SqlStringGenerationContext sqlStringGenerationContext =
					SqlStringGenerationContextImpl.forTests( database.getJdbcEnvironment() );
			final String[] createCommands = new DB2Dialect().getTableExporter().getSqlCreateStrings( table, metadata,
					sqlStringGenerationContext
			);
			assertThat( createCommands[0], containsString( "sequence_name varchar(255) not null" ) );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

}
