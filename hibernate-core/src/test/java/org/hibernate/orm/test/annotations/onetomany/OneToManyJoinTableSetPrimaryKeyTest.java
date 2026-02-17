/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetomany;

import java.util.List;
import java.util.Locale;
import java.util.Set;

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
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.AssertionsKt.assertNotNull;

@BaseUnitTest
public class OneToManyJoinTableSetPrimaryKeyTest {
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
	public void testJoinTablePrimaryKeyUsesOnlyTargetForeignKey() {
		final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
				.addAnnotatedClass( Publisher.class )
				.addAnnotatedClass( Author.class )
				.buildMetadata();
		metadata.orderColumns( true );
		metadata.validate();

		final List<String> commands = new SchemaCreatorImpl( ssr ).generateCreationCommands( metadata, false );
		final String joinTableCreateCommand;
		String found = null;
		for ( String command : commands ) {
			final String lowerCaseCommand = command.toLowerCase( Locale.ROOT );
			if ( lowerCaseCommand.contains( "create table" )
				&& lowerCaseCommand.contains( "publishedauthors" ) ) {
				found = lowerCaseCommand;
				break;
			}
		}
		assertNotNull( found );

		assertThat( found )
				.containsPattern( "\"?publisher_id\"?\\s+[^,]*\\bnot\\s+null\\b" )
				.containsPattern( "\"?authors_ssn\"?\\s+[^,]*\\bnot\\s+null\\b" )
				.containsPattern( "\\bprimary\\s+key\\s*\\(\\s*\"?authors_ssn\"?\\s*\\)" )
				.doesNotContainPattern( "\"?authors_ssn\"?\\s+[^,]*\\bunique\\b" )
				.doesNotContainPattern(
						"\\bprimary\\s+key\\s*\\(\\s*\"?publisher_id\"?\\s*,\\s*\"?authors_ssn\"?\\s*\\)"
				);
	}

	@Entity(name = "Publisher")
	public static class Publisher {
		@Id
		private Long id;

		@OneToMany
		@JoinTable(name = "PublishedAuthors")
		private Set<Author> authors;
	}

	@Entity(name = "Author")
	public static class Author {
		@Id
		private String ssn;
	}
}
