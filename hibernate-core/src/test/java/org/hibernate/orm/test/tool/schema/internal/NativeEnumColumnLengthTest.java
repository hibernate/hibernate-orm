package org.hibernate.orm.test.tool.schema.internal;

import java.sql.Types;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.mapping.Column;
import org.hibernate.tool.schema.extract.internal.ColumnInformationImpl;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.tool.schema.internal.ColumnDefinitions.hasMatchingLength;
import static org.hibernate.tool.schema.internal.ColumnDefinitions.hasMatchingType;

/**
 * Verifies that the declared length of a native {@code enum} column is not
 * compared against the size reported by JDBC, which for MySQL is just the
 * length of the longest enumerated value.
 */
@JiraKey("HHH-20850")
public class NativeEnumColumnLengthTest {
	private StandardServiceRegistry ssr;

	@BeforeEach
	public void setUp() {
		ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, MySQLDialect.class.getName() )
				.applySetting( AvailableSettings.ALLOW_METADATA_ON_BOOT, false )
				.build();
	}

	@AfterEach
	public void tearDown() {
		StandardServiceRegistryBuilder.destroy( ssr );
	}

	@Test
	public void testExplicitLengthDoesNotCauseMismatch() {
		final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
				.addAnnotatedClass( Widget.class )
				.buildMetadata();
		metadata.orderColumns( false );
		metadata.validate();
		final Dialect dialect = ssr.getService( JdbcEnvironment.class ).getDialect();

		final Column column = metadata.getEntityBinding( Widget.class.getName() )
				.getTable()
				.getColumn( Identifier.toIdentifier( "status" ) );
		assertThat( column.getSqlType( metadata ) ).startsWith( "enum" );

		// what MySQL Connector/J reports for 'enum ('ARCHIVED','DRAFT','PUBLISHED')'
		final ColumnInformationImpl columnInformation = new ColumnInformationImpl(
				null,
				Identifier.toIdentifier( "status" ),
				Types.CHAR,
				"ENUM",
				"PUBLISHED".length(),
				0,
				false
		);

		assertThat( hasMatchingType( column, columnInformation, metadata, dialect ) ).isTrue();
		assertThat( hasMatchingLength( column, columnInformation, metadata, dialect ) ).isTrue();
	}

	@Entity(name = "Widget")
	public static class Widget {
		@Id
		private Long id;

		@Enumerated(EnumType.STRING)
		@jakarta.persistence.Column(nullable = false, length = 20)
		private Status status;
	}

	public enum Status {
		ARCHIVED,
		DRAFT,
		PUBLISHED
	}
}
