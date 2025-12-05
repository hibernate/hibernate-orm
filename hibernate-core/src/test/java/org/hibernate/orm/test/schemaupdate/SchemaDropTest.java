/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-10605")
@RequiresDialect(value = HSQLDialect.class)
@ServiceRegistry
@DomainModel(annotatedClasses = SchemaDropTest.MyEntity.class)
public class SchemaDropTest implements ExceptionHandler {
	@BeforeEach
	public void setUp(DomainModelScope modelScope) throws Exception {
		var model = modelScope.getDomainModel();
		model.orderColumns( false );
		model.validate();
	}

	@Test
	public void testDropSequence(ServiceRegistryScope registryScope, DomainModelScope modelScope) {
		getSchemaDropper( registryScope ).doDrop(
				modelScope.getDomainModel(),
				options( registryScope ),
				ContributableMatcher.ALL,
				getSourceDescriptor(),
				getTargetDescriptor()
		);
	}

	private SchemaDropper getSchemaDropper(ServiceRegistryScope registryScope) {
		return registryScope.getRegistry().requireService( SchemaManagementTool.class ).getSchemaDropper( null );
	}

	private ExecutionOptions options(ServiceRegistryScope registryScope) {
		return new ExecutionOptions() {
			@Override
			public Map<String, Object> getConfigurationValues() {
				return registryScope.getRegistry().requireService( ConfigurationService.class ).getSettings();
			}

			@Override
			public boolean shouldManageNamespaces() {
				return false;
			}

			@Override
			public ExceptionHandler getExceptionHandler() {
				return SchemaDropTest.this;
			}
		};
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
