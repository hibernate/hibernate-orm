/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
import org.hibernate.tool.schema.spi.ContributableMatcher;
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
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseUnitTestCase;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-10605")
@RequiresDialect(value = HSQLDialect.class)
public class SchemaDropTest extends BaseUnitTestCase implements ExecutionOptions, ExceptionHandler {
	protected ServiceRegistry serviceRegistry;
	protected MetadataImplementor metadata;

	@Before
	public void setUp() {
		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( Environment.getProperties() );
		metadata = (MetadataImplementor) new MetadataSources( serviceRegistry )
				.addAnnotatedClass( MyEntity.class ).buildMetadata();
		metadata.orderColumns( false );
		metadata.validate();
	}

	@Test
	public void testDropSequence() {
		getSchemaDropper().doDrop(
				metadata,
				this,
				ContributableMatcher.ALL,
				getSourceDescriptor(),
				getTargetDescriptor()
		);
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
	public Map<String,Object> getConfigurationValues() {
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
