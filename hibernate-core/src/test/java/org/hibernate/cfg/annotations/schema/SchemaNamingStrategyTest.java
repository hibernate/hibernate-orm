/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations.schema;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Properties;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.annotations.schema.entities.dynamic.Dynamic;
import org.hibernate.cfg.annotations.schema.entities.dynamic.DynamicSchemaFilter;
import org.hibernate.cfg.annotations.schema.entities.global.Global;
import org.hibernate.cfg.annotations.schema.entities.global.GlobalSchemaFilter;
import org.hibernate.cfg.schema.SchemaNameResolver;
import org.hibernate.cfg.schema.SchemaNamingDefaultProvider;
import org.hibernate.cfg.schema.SchemaNamingProviderLocator;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.jpa.test.PersistenceUnitInfoAdapter;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase.TestingPersistenceUnitDescriptorImpl;
import org.hibernate.query.NativeQueryWithParenthesesTest.Person;

import javax.persistence.EntityManagerFactory;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaExport.Action;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.junit.Test;

/**
 * Test for HHH-12278
 *
 * @author Benoit Besson
 */
public class SchemaNamingStrategyTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] { Global.class, Dynamic.class };
	}

	@Test
	public void testExportSchemaName() throws IOException {

		// instanciate schema name resolver
		SchemaNameResolver resolver = new SchemaNameResolverImpl();

		// wrap into default provider
		SchemaNamingDefaultProvider provider = new SchemaNamingDefaultProvider(resolver);

		// register provider to Hibernate core
		SchemaNamingProviderLocator.setInstance(provider);

		// set dynamic schema name
		Properties properties = System.getProperties();
		properties.setProperty(SchemaNameResolverImpl.SCHEMA_DYNAMIC_NAME_PROPERTY_NAME,
				SchemaNameResolverImpl.getDynamicSchemaName());

		// export entites filtered in the global backage
		exportSchema(GlobalSchemaFilter.class);
		// export entites filtered in the global dynamic
		exportSchema(DynamicSchemaFilter.class);

		// doInJPA( this::entityManagerFactory, entityManager -> {
		// entityManager.createNativeQuery(
		//
		// } );
	}
	
	//copied from superclass as it is not provided as protected
	private PersistenceUnitDescriptor buildPersistenceUnitDescriptor() {
		return new TestingPersistenceUnitDescriptorImpl( getClass().getSimpleName() );
	}

	private void exportSchema(Class<? extends SchemaFilter> schemaFilterProviderClass) throws IOException {

		// set export filter
		Properties properties = System.getProperties();
		properties.put("jpa.properties.hibernate.hbm2ddl.schema_filter_provider", schemaFilterProviderClass.getName());

		// configure NamingStrategy
		{
			EntityManagerFactoryBuilderImpl builder = (EntityManagerFactoryBuilderImpl) Bootstrap
					.getEntityManagerFactoryBuilder(buildPersistenceUnitDescriptor(), buildSettings());
			// extract metadata
			MetadataImplementor metadata = builder.getMetadata();

			// export schema to check
			SchemaExport schemaExport = new SchemaExport();
			schemaExport.setFormat(true);
			schemaExport.setDelimiter(";");
			schemaExport.setHaltOnError(true);
			schemaExport.setImportFiles(null);
			File outputFile = File.createTempFile("schema-screate", "sql");
			schemaExport.setOutputFile(outputFile.getAbsolutePath());

			// write to stdout and file
			schemaExport.execute(EnumSet.of(TargetType.SCRIPT, TargetType.STDOUT), Action.CREATE, metadata);

			// TODO : add assertion for expected file content

		}

	}

}
