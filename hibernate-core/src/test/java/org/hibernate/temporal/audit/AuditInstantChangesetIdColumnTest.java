package org.hibernate.temporal.audit;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Audited;
import org.hibernate.annotations.Changelog;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsJdbcTimestampJdbcType;
import org.junit.jupiter.api.Test;

import static org.hibernate.boot.model.internal.AuditHelper.CHANGESET_ID;
import static org.hibernate.boot.model.internal.AuditHelper.INVALIDATING_CHANGESET_ID;
import static org.hibernate.cfg.JdbcSettings.ALLOW_METADATA_ON_BOOT;
import static org.hibernate.cfg.JdbcSettings.DIALECT;
import static org.hibernate.cfg.StateManagementSettings.AUDIT_STRATEGY;
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
		assertSame( jdbcType( changelogKey ), jdbcType( auditColumn( metadata, CHANGESET_ID ) ) );
		assertSame( jdbcType( changelogKey ), jdbcType( auditColumn( metadata, INVALIDATING_CHANGESET_ID ) ) );
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
		for ( var name : new String[] { CHANGESET_ID, INVALIDATING_CHANGESET_ID } ) {
			final var column = auditColumn( metadata, name );
			assertEquals( "timestamp(9)", column.getSqlType( metadata ) );
			assertInstanceOf( TimestampUtcAsJdbcTimestampJdbcType.class, jdbcType( column ) );
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
		Instant id = Instant.now();

		@Changelog.Timestamp
		Instant timestamp = id;
	}
}
