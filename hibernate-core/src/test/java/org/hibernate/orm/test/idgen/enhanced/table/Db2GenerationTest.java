/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.enhanced.table;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Table;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;


@SuppressWarnings("JUnitMalformedDeclaration")
@org.hibernate.testing.orm.junit.ServiceRegistry
@RequiresDialect( DB2Dialect.class )
public class Db2GenerationTest {
	@Test
	@JiraKey( value = "HHH-9850" )
	public void testNewGeneratorTableCreationOnDb2(ServiceRegistryScope registryScope) {
		final StandardServiceRegistry ssr = registryScope.getRegistry();
		final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr ).buildMetadata();
		final Database database = metadata.getDatabase();
		final Namespace databaseNamespace = database.getDefaultNamespace();

		assertThat( databaseNamespace.getTables() ).isEmpty();

		final TableGenerator generator = new TableGenerator();
		generator.configure(
				new GeneratorCreationContextImpl( metadata, ssr ),
				new Properties()
		);
		generator.registerExportables( database );

		assertThat( databaseNamespace.getTables() ).hasSize( 1 );

		final Table table = databaseNamespace.getTables().iterator().next();
		SqlStringGenerationContext sqlStringGenerationContext =
				SqlStringGenerationContextImpl.forTests( database.getJdbcEnvironment() );
		final String[] createCommands = new DB2Dialect().getTableExporter().getSqlCreateStrings( table, metadata,
				sqlStringGenerationContext
		);
		assertThat( createCommands[0] ).contains( "sequence_name varchar(255) not null" );
	}

	private static class GeneratorCreationContextImpl implements GeneratorCreationContext {
		private final MetadataImplementor metadata;
		private final StandardServiceRegistry ssr;

		public GeneratorCreationContextImpl(MetadataImplementor metadata, StandardServiceRegistry ssr) {
			this.metadata = metadata;
			this.ssr = ssr;
		}

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
	}
}
