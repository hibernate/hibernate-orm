/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.EnumSet;
import java.util.Map;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.SourceType;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaDropper;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.ScriptSourceInput;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;

import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-10605")
@RequiresDialect(value = HSQLDialect.class)
public class SchemaDropTest extends BaseUnitTestCase implements ExecutionOptions, ExceptionHandler {
	protected ServiceRegistry serviceRegistry;
	protected MetadataImplementor metadata;

	@Before
	public void setUp() {
		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( Environment.getProperties() );
		metadata = (MetadataImplementor) new MetadataSources( serviceRegistry )
				.addAnnotatedClass( MyEntity.class ).buildMetadata();
		metadata.validate();
	}

	@Test
	public void testDropSequence() {
		getSchemaDropper()
				.doDrop( metadata, this, getSourceDescriptor(), getTargetDescriptor() );
	}

	private SchemaDropper getSchemaDropper() {
		return serviceRegistry.getService( SchemaManagementTool.class ).getSchemaDropper( null );
	}

	private TargetDescriptor getTargetDescriptor() {
		return new TargetDescriptor() {
			@Override
			public EnumSet<TargetType> getTargetTypes() {
				return EnumSet.of( TargetType.DATABASE );
			}

			@Override
			public ScriptTargetOutput getScriptTargetOutput() {
				return null;
			}
		};
	}

	private SourceDescriptor getSourceDescriptor() {
		return new SourceDescriptor() {
			@Override
			public SourceType getSourceType() {
				return SourceType.METADATA;
			}

			@Override
			public ScriptSourceInput getScriptSourceInput() {
				return null;
			}
		};
	}

	@Override
	public Map getConfigurationValues() {
		return serviceRegistry.getService( ConfigurationService.class ).getSettings();
	}

	@Override
	public boolean shouldManageNamespaces() {
		return false;
	}

	@Override
	public ExceptionHandler getExceptionHandler() {
		return this;
	}

	@Override
	public void handleException(CommandAcceptanceException exception) {
		throw exception;
	}

	@Entity(name = "MyEntity")
	public static class MyEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		Long id;
	}
}
