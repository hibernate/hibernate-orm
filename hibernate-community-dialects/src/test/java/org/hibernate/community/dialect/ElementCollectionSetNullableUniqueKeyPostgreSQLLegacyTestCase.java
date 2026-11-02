package org.hibernate.community.dialect;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BaseUnitTest
@RequiresDialect(PostgreSQLLegacyDialect.class)
public class ElementCollectionSetNullableUniqueKeyPostgreSQLLegacyTestCase {


	@Test
	void keepsStandardUniqueKeyForPostgreSQL14() {
		final var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( JdbcSettings.DIALECT, ElementCollectionSetNullableUniqueKeyPostgreSQLLegacyTestCase.PostgreSQL14Dialect.class )
				.build();
		try {
			final var commands = generateCreationCommands( registry, buildMetadata( registry ) );

			assertTrue( hasUniqueTupleDefinition( commands, "book_topics", "book_isbn", "topics" ) );
			assertFalse( hasNullsNotDistinctUniqueTupleDefinition( commands, "book_topics", "book_isbn", "topics" ) );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( registry );
		}
	}

	public static class PostgreSQL14Dialect extends PostgreSQLLegacyDialect {
		public PostgreSQL14Dialect() {
			super( DatabaseVersion.make( 14 ) );
		}
	}

	private static List<String> generateCreationCommands(StandardServiceRegistry registry, MetadataImplementor metadata) {
		return new SchemaCreatorImpl( registry )
				.generateCreationCommands( metadata, false );
	}

	private static boolean hasUniqueTupleDefinition(
			List<String> commands,
			String tableName,
			String firstColumn,
			String secondColumn) {
		final String uniqueConstraintPattern = "\\bunique(?:\\s+nulls\\s+not\\s+distinct)?\\s*\\(\\s*"
											+ columnPattern( firstColumn )
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

	private static boolean hasNullsNotDistinctUniqueTupleDefinition(
			List<String> commands,
			String tableName,
			String firstColumn,
			String secondColumn) {
		final String uniqueConstraintPattern = "\\bunique\\s+nulls\\s+not\\s+distinct\\s*\\(\\s*"
											+ columnPattern( firstColumn ) + "\\s*,\\s*" + columnPattern( secondColumn ) + "\\s*\\)";

		for ( String command : commands ) {
			final String lowerCaseCommand = command.toLowerCase( Locale.ROOT );
			if ( lowerCaseCommand.contains( tableName )
				&& lowerCaseCommand.matches( ".*" + uniqueConstraintPattern + ".*" ) ) {
				return true;
			}
		}
		return false;
	}

	private static MetadataImplementor buildMetadata(StandardServiceRegistry registry) {
		final var metadata = (MetadataImplementor)
				new MetadataSources( registry )
						.addAnnotatedClass( ElementCollectionSetNullableUniqueKeyPostgreSQLLegacyTestCase.Book.class )
						.buildMetadata();
		metadata.orderColumns( true );
		metadata.validate();
		return metadata;
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
