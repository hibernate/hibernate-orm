/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations.schema;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.spi.PersistenceUnitInfo;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.annotations.BaseUnitTestCase;
import org.hibernate.cfg.annotations.Test;
import org.hibernate.cfg.annotations.schema.entities.dynamic.DynamicSchemaFilter;
import org.hibernate.cfg.annotations.schema.entities.global.GlobalSchemaFilter;
import org.hibernate.cfg.annotations.schema.entities.noscope.UnscopedSchemaFilter;
import org.hibernate.cfg.schema.SchemaNameResolver;
import org.hibernate.cfg.schema.SchemaNamingDefaultProvider;
import org.hibernate.cfg.schema.SchemaNamingProviderLocator;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.test.MyNamingStrategy;
import org.hibernate.jpa.test.PersistenceUnitInfoAdapter;
import org.hibernate.jpa.test.ejb3configuration.EntityManagerFactory;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaExport.Action;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaFilterProvider;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import fr.artal.terea.core.exception.WrappedAsUncheckedException;

/**
 * Test for HHH-12278
 *
 * @author Benoit Besson
 */
public class SchemaNamingStrategyTest extends BaseUnitTestCase {
	// TODO : how to test it with a simple test case ??

	@Test
	public void testExportSchemaName() {

		// instanciate schema name resolver
		SchemaNameResolver resolver = new SchemaNameResolverImpl();

		// wrap into default provider
		SchemaNamingDefaultProvider provider = new SchemaNamingDefaultProvider( resolver );

		// register provider to Hibernate core
		SchemaNamingProviderLocator.setInstance( provider );

		// set dynamic schema name
		Properties properties = System.getProperties();
		properties.setProperty( SchemaNameResolverImpl.SCHEMA_DYNAMIC_NAME_PROPERTY_NAME, SchemaNameResolverImpl.getDynamicSchemaName() );

		//export entites filtered in the global backage
		exportSchema(GlobalSchemaFilter.class);
		//export entites filtered in the global dynamic
		exportSchema(DynamicSchemaFilter.class);
	}

	private void exportSchema(Class<? extends SchemaFilter> schemaFilterProviderClass) {

		//set export filter
		Properties properties = System.getProperties();
		properties.put("jpa.properties.hibernate.hbm2ddl.schema_filter_provider",schemaFilterProviderClass.getName());
		
		// configure NamingStrategy
		{
			PersistenceUnitInfoAdapter adapter = new PersistenceUnitInfoAdapter();
			EntityManagerFactoryBuilderImpl builder = (EntityManagerFactoryBuilderImpl) Bootstrap.getEntityManagerFactoryBuilder(
					adapter,
					Collections.emptyMap() );
			final EntityManagerFactory emf = builder.build();
			try {

				// extract metadata
				MetadataImplementor metadata = builder.getMetadata();

				// export schema to check
				SchemaExport schemaExport = new SchemaExport();
				schemaExport.setFormat( true );
				schemaExport.setDelimiter( ";" );
				schemaExport.setHaltOnError( true );
				schemaExport.setImportFiles( null );
				File outputFile = File.createTempFile( "schema-screate", "sql" );
				schemaExport.setOutputFile( outputFile.getAbsolutePath() );

				// write to stdout and file
				schemaExport.execute( EnumSet.of( TargetType.SCRIPT, TargetType.STDOUT ), Action.CREATE, metadata );

				// TODO : add assertion for expected file content
			}
			finally {
				if ( emf != null ) {
					emf.close();
				}
			}
		}
	}

}
