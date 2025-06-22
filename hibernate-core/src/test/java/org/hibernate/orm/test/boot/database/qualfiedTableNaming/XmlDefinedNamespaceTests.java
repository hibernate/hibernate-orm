/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.database.qualfiedTableNaming;

import java.io.StringWriter;
import java.util.EnumSet;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.exec.ScriptTargetOutputToWriter;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.TargetDescriptor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @implNote Limited to H2 so that we can expect and assert based on a consistent qualifier pattern.  See
 * {@linkplain DefaultCatalogAndSchemaTest} for more complete testing of XML namespace application
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@RequiresDialect( H2Dialect.class )
public class XmlDefinedNamespaceTests {
	@ServiceRegistry()
	@DomainModel( xmlMappings = "mappings/models/namespace/no-namespace.xml" )
	@Test
	public void testNoNamespace(DomainModelScope domainModelScope, ServiceRegistryScope serviceRegistryScope) {
		// tests case where there is no namespace - explicit or implicit
		final MetadataImplementor domainModel = domainModelScope.getDomainModel();
		final StandardServiceRegistry serviceRegistry = serviceRegistryScope.getRegistry();
		final String script = generateScript( SchemaExport.Action.CREATE, domainModel, serviceRegistry );
		assertThat( script ).containsIgnoringCase( "create table simple_entity" );
	}

	@ServiceRegistry()
	@DomainModel( xmlMappings = "mappings/models/namespace/defaults-namespace.xml" )
	@Test
	public void testXmlDefaultsNamespace(DomainModelScope domainModelScope, ServiceRegistryScope serviceRegistryScope) {
		// tests case where there is no namespace - explicit or implicit
		final MetadataImplementor domainModel = domainModelScope.getDomainModel();
		final StandardServiceRegistry serviceRegistry = serviceRegistryScope.getRegistry();
		final String script = generateScript( SchemaExport.Action.CREATE, domainModel, serviceRegistry );
		assertThat( script ).containsIgnoringCase( "create table defaults_schema.simple_entity" );
	}

	@ServiceRegistry()
	@DomainModel( xmlMappings = "mappings/models/namespace/file-namespace.xml" )
	@Test
	public void testXmlFileNamespace(DomainModelScope domainModelScope, ServiceRegistryScope serviceRegistryScope) {
		// tests case where there is no namespace - explicit or implicit
		final MetadataImplementor domainModel = domainModelScope.getDomainModel();
		final StandardServiceRegistry serviceRegistry = serviceRegistryScope.getRegistry();
		final String script = generateScript( SchemaExport.Action.CREATE, domainModel, serviceRegistry );
		assertThat( script ).containsIgnoringCase( "create table file_schema.simple_entity" );
	}

	public static class SimpleEntity {
		private Integer id;
		private String name;
	}


	private String generateScript(
			SchemaExport.Action action,
			MetadataImplementor domainModel,
			StandardServiceRegistry serviceRegistry) {
		SchemaExport schemaExport = new SchemaExport();
		schemaExport.setFormat( true );
		schemaExport.setDelimiter( ";" );
		StringWriter writer = new StringWriter();
		schemaExport.doExecution(
				action,
				false,
				domainModel,
				serviceRegistry,
				new TargetDescriptor() {
					@Override
					public EnumSet<TargetType> getTargetTypes() {
						return EnumSet.of( TargetType.SCRIPT );
					}

					@Override
					public ScriptTargetOutput getScriptTargetOutput() {
						return new ScriptTargetOutputToWriter( writer ) {
							@Override
							public void accept(String command) {
								super.accept( command );
							}
						};
					}
				}
		);
		return writer.toString();
	}

}
