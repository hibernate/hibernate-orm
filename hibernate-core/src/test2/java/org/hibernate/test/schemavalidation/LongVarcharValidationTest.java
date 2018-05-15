/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemavalidation;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Type;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
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
 * @author Steve Ebersole
 */
@TestForIssue(jiraKey = "HHH-9693")
@RunWith(Parameterized.class)
public class LongVarcharValidationTest implements ExecutionOptions {
	@Parameterized.Parameters
	public static Collection<String> parameters() {
		return Arrays.asList(
				new String[] {JdbcMetadaAccessStrategy.GROUPED.toString(), JdbcMetadaAccessStrategy.INDIVIDUALLY.toString()}
		);
	}

	@Parameterized.Parameter
	public String jdbcMetadataExtractorStrategy;

	private StandardServiceRegistry ssr;

	@Before
	public void beforeTest() {
		ssr = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY, jdbcMetadataExtractorStrategy )
				.build();
	}

	@After
	public void afterTest() {
		if ( ssr != null ) {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	public void testValidation() {
		MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
				.addAnnotatedClass( Translation.class )
				.buildMetadata();
		metadata.validate();


		// create the schema
		createSchema( metadata );

		try {
			doValidation( metadata );
		}
		finally {
			dropSchema( metadata );
		}
	}

	private void doValidation(MetadataImplementor metadata) {
		ssr.getService( SchemaManagementTool.class ).getSchemaValidator( null ).doValidation(
				metadata,
				this
		);
	}

	private void createSchema(MetadataImplementor metadata) {
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

	private void dropSchema(MetadataImplementor metadata) {
		ssr.getService( SchemaManagementTool.class ).getSchemaDropper( null ).doDrop(
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

@Entity(name = "Translation")
public static class Translation {
	@Id
	public Integer id;
	@Type(type = "text")
	String text;
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
