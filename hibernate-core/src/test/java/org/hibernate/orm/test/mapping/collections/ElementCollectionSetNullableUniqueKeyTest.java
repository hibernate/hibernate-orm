/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BaseUnitTest
@SkipForDialect( dialectClass = SybaseDialect.class, matchSubTypes = true,
		reason = "Sybase does not support unique constraints on nullable columns" )
public class ElementCollectionSetNullableUniqueKeyTest {
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
	void createsUniqueKeyForSetAndPrimaryKeyForOrderedList() {
		final var metadata = (MetadataImplementor)
				new MetadataSources( ssr )
						.addAnnotatedClass( Book.class )
						.buildMetadata();
		metadata.orderColumns( true );
		metadata.validate();

		final List<String> commands =
				new SchemaCreatorImpl( ssr )
						.generateCreationCommands( metadata, false );

		final String topicsTableCreateCommand = findCreateTableCommand( commands, "book_topics" );
		assertNotNull( topicsTableCreateCommand );

		assertThat( topicsTableCreateCommand )
				.containsPattern( "\"?book_isbn\"?\\s+[^,]*\\bnot\\s+null\\b" )
				.doesNotContainPattern( "\\bprimary\\s+key\\b" );
		assertTrue( hasUniqueTupleDefinition( commands, "book_topics", "book_isbn", "topics" ) );

		final String commentsTableCreateCommand = findCreateTableCommand( commands, "book_comments" );
		assertNotNull( commentsTableCreateCommand );

		assertThat( commentsTableCreateCommand )
				.containsPattern( "\"?book_isbn\"?\\s+[^,]*\\bnot\\s+null\\b" )
				.containsPattern( "\"?comments_order\"?\\s+.*\\bnot\\s+null\\b.*\\bcheck\\s" )
				.containsPattern( "\\bcheck\\s*\\(\\s*\\(?\\s*\"?comments_order\"?\\s*>=\\s*0\\s*\\)?\\s*\\)" )
				.containsPattern(
						"\\bprimary\\s+key\\s*\\(\\s*(?:\"?book_isbn\"?\\s*,\\s*\"?comments_order\"?|\"?comments_order\"?\\s*,\\s*\"?book_isbn\"?)\\s*\\)"
				)
				.doesNotContainPattern( "\\bunique\\s*\\(\\s*\"?book_isbn\"?\\s*,\\s*\"?comments\"?\\s*\\)" );
		assertFalse( hasUniqueTupleDefinition( commands, "book_comments", "book_isbn", "comments" ) );
	}

	private static String findCreateTableCommand(List<String> commands, String tableName) {
		for ( String command : commands ) {
			final String lowerCaseCommand = command.toLowerCase( Locale.ROOT );
			if ( lowerCaseCommand.contains( "create table" ) && lowerCaseCommand.contains( tableName ) ) {
				return lowerCaseCommand;
			}
		}
		return null;
	}

	private static boolean hasUniqueTupleDefinition(
			List<String> commands,
			String tableName,
			String firstColumn,
			String secondColumn) {
		final String uniqueConstraintPattern = "\\bunique\\s*\\(\\s*" + columnPattern( firstColumn )
				+ "\\s*,\\s*" + columnPattern( secondColumn ) + "\\s*\\)";
		final String uniqueIndexPattern = "\\bcreate\\s+unique(?:\\s+\\w+)*\\s+index\\b[^\\(]*\\(\\s*"
				+ columnPattern( firstColumn ) + "\\s*,\\s*" + columnPattern( secondColumn ) + "\\s*\\)";

		for ( String command : commands ) {
			final String lowerCaseCommand = command.toLowerCase( Locale.ROOT );
			if ( lowerCaseCommand.contains( tableName )
					&& ( lowerCaseCommand.matches( ".*" + uniqueConstraintPattern + ".*" )
						|| lowerCaseCommand.matches( ".*" + uniqueIndexPattern + ".*" ) ) ) {
				return true;
			}
		}
		return false;
	}

	private static String columnPattern(String columnName) {
		return "[\"`\\[]?" + columnName + "[\"`\\]]?";
	}

	@Entity(name = "Book")
	public static class Book {
		@Id
		private String isbn;

		@ElementCollection
		private Set<String> topics;

		@ElementCollection
		@OrderColumn
		private List<String> comments;
	}
}
