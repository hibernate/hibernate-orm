package org.hibernate.temporal.audit;

import java.time.Instant;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Audited;
import org.hibernate.annotations.Changelog;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsJdbcTimestampJdbcType;
import org.junit.jupiter.api.Test;

import static org.hibernate.boot.model.internal.AuditHelper.CHANGESET_ID;
import static org.hibernate.boot.model.internal.AuditHelper.INVALIDATING_CHANGESET_ID;
import static org.hibernate.cfg.JdbcSettings.ALLOW_METADATA_ON_BOOT;
import static org.hibernate.cfg.JdbcSettings.DIALECT;
import static org.hibernate.cfg.StateManagementSettings.AUDIT_STRATEGY;
import static org.hibernate.cfg.MappingSettings.PREFERRED_INSTANT_JDBC_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The type of the changeset id columns of an audit table when
 * the changeset identifiers are timestamps.
 */
@JiraKey("HHH-21233")
class AuditInstantChangesetIdColumnTest {

	@Test
	@ServiceRegistry(settings = @Setting(name = AUDIT_STRATEGY, value = "VALIDITY"))
	@DomainModel(annotatedClasses = { Book.class, Changeset.class })
	void changesetIdHasTypeOfChangelogKey(DomainModelScope scope) {
		final var metadata = scope.getDomainModel();
		final var changelogKey =
				metadata.getEntityBinding( Changeset.class.getName() )
						.getIdentifier().getColumns().get( 0 );
		// these columns are foreign keys referencing the changelog
		for ( var name : List.of( CHANGESET_ID, INVALIDATING_CHANGESET_ID ) ) {
			final var column = auditColumn( metadata, name );
			assertSame( jdbcType( changelogKey ), jdbcType( column ) );
			assertEquals( changelogKey.getSqlType( metadata ), column.getSqlType( metadata ) );
		}
	}

	@Test
	@ServiceRegistry(settings = {
			@Setting(name = DIALECT, value = "org.hibernate.dialect.OracleDialect"),
			@Setting(name = ALLOW_METADATA_ON_BOOT, value = "false"),
			@Setting(name = AUDIT_STRATEGY, value = "VALIDITY")
	})
	@DomainModel(annotatedClasses = Book.class)
	void changesetIdIsUtcTimestampOnOracle(DomainModelScope scope) {
		final var metadata = scope.getDomainModel();
		// Oracle does not allow a timestamp with time zone in a primary key
		for ( var name : List.of( CHANGESET_ID, INVALIDATING_CHANGESET_ID ) ) {
			final var column = auditColumn( metadata, name );
			assertEquals( "timestamp(9)", column.getSqlType( metadata ) );
			assertInstanceOf( TimestampUtcAsJdbcTimestampJdbcType.class, jdbcType( column ) );
		}
	}

	@Test
	@ServiceRegistry(settings = {
			@Setting(name = DIALECT, value = "org.hibernate.dialect.OracleDialect"),
			@Setting(name = ALLOW_METADATA_ON_BOOT, value = "false"),
			@Setting(name = AUDIT_STRATEGY, value = "VALIDITY"),
			@Setting(name = PREFERRED_INSTANT_JDBC_TYPE, value = "TIMESTAMP")
	})
	@DomainModel(annotatedClasses = Book.class)
	void keepsPreferredTimestampBinderOnOracle(DomainModelScope scope) {
		for ( var name : List.of( CHANGESET_ID, INVALIDATING_CHANGESET_ID ) ) {
			final var column = auditColumn( scope.getDomainModel(), name );
			assertEquals( "timestamp(9)", column.getSqlType( scope.getDomainModel() ) );
			assertSame( TimestampJdbcType.INSTANCE, jdbcType( column ) );
		}
	}

	@Test
	@ServiceRegistry(settings = @Setting(name = AUDIT_STRATEGY, value = "VALIDITY"))
	@DomainModel(annotatedClasses = { Book.class, ChangesetWithUniqueKey.class })
	void referencesTheAnnotatedChangesetKey(DomainModelScope scope) {
		final var metadata = scope.getDomainModel();
		final var key = metadata.getEntityBinding( ChangesetWithUniqueKey.class.getName() )
				.getProperty( "revision" );
		final var auditTable = metadata.getEntityBinding( Book.class.getName() ).getRootClass().getAuxiliaryTable();
		assertEquals( 2, auditTable.getForeignKeyCollection().size() );
		for ( var fk : auditTable.getForeignKeyCollection() ) {
			assertEquals( key.getColumns(), fk.getReferencedColumns() );
			assertSame( jdbcType( key.getColumns().get( 0 ) ), jdbcType( fk.getColumn( 0 ) ) );
		}
	}

	private static Column auditColumn(MetadataImplementor metadata, String name) {
		return metadata.getEntityBinding( Book.class.getName() ).getRootClass().getAuxiliaryColumn( name );
	}

	private static JdbcType jdbcType(Column column) {
		return ( (BasicValue) column.getValue() ).resolve().getJdbcType();
	}

	@Audited
	@Entity(name = "Book")
	static class Book {
		@Id
		int id;
	}

	@Changelog
	@Entity(name = "Changeset")
	static class Changeset {
		@Id
		@Changelog.ChangesetId
		@JdbcTypeCode(SqlTypes.TIMESTAMP)
		@jakarta.persistence.Column(secondPrecision = 3)
		Instant id = Instant.now();

		@Changelog.Timestamp
		Instant timestamp = id;
	}

	@Changelog
	@Entity(name = "ChangesetWithUniqueKey")
	static class ChangesetWithUniqueKey {
		@Id
		long id;

		@Changelog.ChangesetId
		@JdbcTypeCode(SqlTypes.TIMESTAMP)
		Instant revision;

		@Changelog.Timestamp
		Instant timestamp;
	}
}
