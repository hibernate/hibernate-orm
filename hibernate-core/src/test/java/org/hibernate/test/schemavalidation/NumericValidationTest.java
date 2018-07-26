/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemavalidation;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.JdbcMetadaAccessStrategy;
import org.hibernate.tool.schema.SourceType;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.ExceptionHandlerLoggedImpl;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.ScriptSourceInput;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;

import org.hibernate.testing.TestForIssue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * @author Jonathan Bregler
 */
@TestForIssue(jiraKey = "HHH-12203")
@RunWith(Parameterized.class)
public class NumericValidationTest implements ExecutionOptions {
	@Parameterized.Parameters
	public static Collection<String> parameters() {
		return Arrays.asList(
				new String[] {
						JdbcMetadaAccessStrategy.GROUPED.toString(),
						JdbcMetadaAccessStrategy.INDIVIDUALLY.toString()
				}
		);
	}

	@Parameterized.Parameter
	public String jdbcMetadataExtractorStrategy;

	private StandardServiceRegistry ssr;
	private MetadataImplementor metadata;

	@Before
	public void beforeTest() {
		ssr = new StandardServiceRegistryBuilder()
				.applySetting(
						AvailableSettings.HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY,
						jdbcMetadataExtractorStrategy
				)
				.build();
		metadata = (MetadataImplementor) new MetadataSources( ssr )
				.addAnnotatedClass( TestEntity.class )
				.buildMetadata();
		metadata.validate();

		try {
			dropSchema();
			// create the schema
			createSchema();
		}
		catch (Exception e) {
			tearDown();
			throw e;
		}
	}

	@After
	public void tearDown() {
		dropSchema();
		if ( ssr != null ) {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	public void testValidation() {
		doValidation();
	}

	private void doValidation() {
		ssr.getService( SchemaManagementTool.class ).getSchemaValidator( null )
				.doValidation( metadata, this );
	}

	private void createSchema() {
		ssr.getService( SchemaManagementTool.class ).getSchemaCreator( null ).doCreation(
				metadata,
				this,
				new SourceDescriptor() {
					@Override
					public SourceType getSourceType() {
						return SourceType.METADATA;
					}

					@Override
					public ScriptSourceInput getScriptSourceInput() {
						return null;
					}
				},
				new TargetDescriptor() {
					@Override
					public EnumSet<TargetType> getTargetTypes() {
						return EnumSet.of( TargetType.DATABASE );
					}

					@Override
					public ScriptTargetOutput getScriptTargetOutput() {
						return null;
					}
				}
		);
	}

	private void dropSchema() {
		new SchemaExport()
				.drop( EnumSet.of( TargetType.DATABASE ), metadata );
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		public Integer id;

		@Column(name = "numberValue")
		BigDecimal number;
	}

	@Override
	public Map getConfigurationValues() {
		return ssr.getService( ConfigurationService.class ).getSettings();
	}

	@Override
	public boolean shouldManageNamespaces() {
		return false;
	}

	@Override
	public ExceptionHandler getExceptionHandler() {
		return ExceptionHandlerLoggedImpl.INSTANCE;
	}
}
