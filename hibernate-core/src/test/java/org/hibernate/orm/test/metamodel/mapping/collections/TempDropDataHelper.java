/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.metamodel.mapping.collections;

import java.util.EnumSet;
import java.util.Map;

import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.SourceType;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.ScriptSourceInput;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;

import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

/**
 * @author Steve Ebersole
 */
public class TempDropDataHelper {
	static void cleanDatabaseSchema(SessionFactoryScope scope, DomainModelScope domainModelScope) {
		final ServiceRegistryImplementor serviceRegistry = scope.getSessionFactory().getServiceRegistry();
		final SchemaManagementTool schemaTool = serviceRegistry.getService( SchemaManagementTool.class );

		final ExecutionOptions executionOptions = new ExecutionOptions() {
			@Override
			public Map getConfigurationValues() {
				return scope.getSessionFactory().getProperties();
			}

			@Override
			public boolean shouldManageNamespaces() {
				return false;
			}

			@Override
			public ExceptionHandler getExceptionHandler() {
				return new ExceptionHandler() {
					@Override
					public void handleException(CommandAcceptanceException exception) {
						throw exception;
					}
				};
			}
		};

		final SourceDescriptor sourceDescriptor = new SourceDescriptor() {
			@Override
			public SourceType getSourceType() {
				return SourceType.METADATA;
			}

			@Override
			public ScriptSourceInput getScriptSourceInput() {
				return null;
			}
		};

		final TargetDescriptor targetDescriptor = new TargetDescriptor() {
			@Override
			public EnumSet<TargetType> getTargetTypes() {
				return EnumSet.of( TargetType.DATABASE );
			}

			@Override
			public ScriptTargetOutput getScriptTargetOutput() {
				return null;
			}
		};

		schemaTool.getSchemaDropper( scope.getSessionFactory().getProperties() ).doDrop(
				domainModelScope.getDomainModel(),
				executionOptions,
				sourceDescriptor,
				targetDescriptor
		);

		schemaTool.getSchemaCreator( scope.getSessionFactory().getProperties() ).doCreation(
				domainModelScope.getDomainModel(),
				executionOptions,
				sourceDescriptor,
				targetDescriptor
		);
	}
}
