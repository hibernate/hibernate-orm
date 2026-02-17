/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytoone;

import java.util.List;
import java.util.Locale;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BaseUnitTest
public class ManyToOneJoinTableForeignKeyNullabilityTest {
	private StandardServiceRegistry ssr;

	@BeforeEach
	public void setUp() {
		ssr = ServiceRegistryUtil.serviceRegistry();
	}

	@AfterEach
	public void tearDown() {
		if ( ssr != null ) {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	public void testJoinTableForeignKeyColumnIsNotNull() {
		final var metadata = (MetadataImplementor)
				new MetadataSources( ssr )
						.addAnnotatedClass( Author.class )
						.addAnnotatedClass( Publisher.class )
						.buildMetadata();
		metadata.orderColumns( true );
		metadata.validate();

		final List<String> commands =
				new SchemaCreatorImpl( ssr )
						.generateCreationCommands( metadata, false );

		String found = null;
		for ( String command : commands ) {
			final String lowerCaseCommand = command.toLowerCase( Locale.ROOT );
			if ( lowerCaseCommand.contains( "create table" )
				&& lowerCaseCommand.contains( "authorpublisher" ) ) {
				found = lowerCaseCommand;
				break;
			}
		}
		assertNotNull( found );

		assertTrue( found.matches( ".*\"?publisher_id\"?\\s+[^,]*\\bnot\\s+null\\b.*" ) );
	}

	@Entity(name = "Author")
	public static class Author {
		@Id
		private String ssn;

		@ManyToOne
		@JoinTable(name = "AuthorPublisher")
		private Publisher publisher;
	}

	@Entity(name = "Publisher")
	public static class Publisher {
		@Id
		private Long id;
	}
}
