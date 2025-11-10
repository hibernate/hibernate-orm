/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.index;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey( value = "HHH-11815")
@ServiceRegistry
@DomainModel(annotatedClasses = ComponentIndexTest.User.class)
public class ComponentIndexTest {

	@Test
	public void testTheIndexIsGenerated(ServiceRegistryScope registryScope, DomainModelScope modelScope) {
		final List<String> commands = new SchemaCreatorImpl( registryScope.getRegistry() )
				.generateCreationCommands( modelScope.getDomainModel(), false );

		assertThatCreateIndexCommandIsGenerated( commands );
	}

	private void assertThatCreateIndexCommandIsGenerated(List<String> commands) {
		boolean createIndexCommandIsGenerated = false;
		for ( String command : commands ) {
			if ( command.toLowerCase().contains( "create index city_index" ) ) {
				createIndexCommandIsGenerated = true;
				break;
			}
		}
		Assertions.assertTrue( createIndexCommandIsGenerated, "Expected create index command not found" );
	}

	@Entity(name = "user")
	@Table(indexes = @Index(name = "city_index", columnList = "city"))
	public static class User {
		@Id
		private Long id;
		@Embedded
		private Address address;
	}

	@Embeddable
	public static class Address {
		private String city;
		private String street;
		private String postalCode;
	}
}
