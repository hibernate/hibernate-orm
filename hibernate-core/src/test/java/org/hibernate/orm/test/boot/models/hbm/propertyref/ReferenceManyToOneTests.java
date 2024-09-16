/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.hbm.propertyref;

import java.io.StringWriter;

import org.hibernate.annotations.PropertyRef;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.internal.util.StringHelper;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.schema.SchemaCreateHelper;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@Jira( "https://hibernate.atlassian.net/browse/HHH-18396" )
public class ReferenceManyToOneTests {
	@Test
	@ServiceRegistry( settings = @Setting( name = MappingSettings.TRANSFORM_HBM_XML, value = "false" ) )
	@DomainModel(xmlMappings = "mappings/models/hbm/propertyref/ref-one-to-one.hbm.xml")
	void testHbm(ServiceRegistryScope registryScope, DomainModelScope domainModelScope) {
		verifySchema( registryScope, domainModelScope );
	}

	@Test
	@FailureExpected( reason = "Support for property-ref pointing to a to-one not yet implemented in annotations nor mapping.xml", jiraKey = "HHH-18396" )
	@ServiceRegistry( settings = @Setting( name = MappingSettings.TRANSFORM_HBM_XML, value = "true" ) )
	@DomainModel(xmlMappings = "mappings/models/hbm/propertyref/ref-one-to-one.hbm.xml")
	void testHbmTransformed(ServiceRegistryScope registryScope, DomainModelScope domainModelScope) {
		verifySchema( registryScope, domainModelScope );
	}

	@Test
	@FailureExpected( reason = "Support for property-ref pointing to a to-one not yet implemented in annotations nor mapping.xml", jiraKey = "HHH-18396" )
	@ServiceRegistry( settings = @Setting( name = MappingSettings.TRANSFORM_HBM_XML, value = "false" ) )
	@DomainModel( annotatedClasses = {Thing.class, Info.class} )
	void testAnnotations(ServiceRegistryScope registryScope, DomainModelScope domainModelScope) {
		verifySchema( registryScope, domainModelScope );
	}

	private void verifySchema(ServiceRegistryScope registryScope, DomainModelScope domainModelScope) {
		final String schemaScript = toSchemaScript( registryScope, domainModelScope );
		assertThat( schemaScript ).doesNotContainIgnoringCase( "owner_id", "info_id" );
		assertThat( StringHelper.count( schemaScript, "foreign key (" ) ).isEqualTo( 1 );
	}

	private String toSchemaScript(ServiceRegistryScope registryScope, DomainModelScope domainModelScope) {
		final StringWriter stringWriter = new StringWriter();
		SchemaCreateHelper.createOnlyToWriter( domainModelScope.getDomainModel(), registryScope.getRegistry(), stringWriter );

		System.out.println( "Schema" );
		System.out.println( "------" );
		System.out.println( stringWriter );

		return stringWriter.toString();
	}

	@Entity(name="Thing")
	@Table(name="things")
	@SuppressWarnings("unused")
	public static class Thing {
		@Id
		private Integer id;
		private String name;
		@OneToOne
		@PropertyRef( "owner" )
		private Info info;
	}

	@Entity(name="Info")
	@Table(name="infos")
	@SuppressWarnings("unused")
	public static class Info {
		@Id
		private Integer id;
		private String name;
		@ManyToOne
		private Thing owner;
	}
}
