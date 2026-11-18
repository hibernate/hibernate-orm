package org.hibernate.orm.test.where.annotations;

import java.util.Collections;
import java.util.EnumSet;

import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Uses separate, scalar mappings to export the candidate tables in the test dialect.
 * The association mappings infer unique constraints and foreign keys that cannot describe
 * a join key shared by several candidates. These schema mappings retain uniqueness of
 * the complete candidate key, including the language, without changing the tested mappings.
 */
final class RestrictedToOneCandidateSchema implements AutoCloseable {

	private final Metadata metadata;

	RestrictedToOneCandidateSchema(SessionFactoryScope scope, Class<?>... types) {
		final var sources = new MetadataSources(
				scope.getMetadataImplementor().getMetadataBuildingOptions().getServiceRegistry() );
		for ( var type : types ) {
			sources.addAnnotatedClass( type );
		}
		metadata = sources.buildMetadata();
		new SchemaExport().setHaltOnError( true ).createOnly( EnumSet.of( TargetType.DATABASE ), metadata );
	}

	void insert(Session session, String table, String columns, Object[]... rows) {
		final var tableMapping = metadata.getDatabase().getDefaultNamespace().locateTable( Identifier.toIdentifier( table ) );
		final String[] columnNames = columns.split( "," );
		final String parameters = String.join( ",", Collections.nCopies( columnNames.length, "?" ) );
		session.doWork( connection -> {
			try ( var statement = connection.prepareStatement(
					"insert into " + table + " (" + columns + ") values (" + parameters + ")" ) ) {
				for ( var row : rows ) {
					for ( int i = 0; i < row.length; i++ ) {
						if ( row[i] == null ) {
							final var column = tableMapping.getColumn( Identifier.toIdentifier( columnNames[i] ) );
							statement.setNull( i + 1, column.getSqlTypeCode( metadata ) );
						}
						else {
							statement.setObject( i + 1, row[i] );
						}
					}
					statement.executeUpdate();
				}
			}
		} );
	}

	@Override
	public void close() {
		new SchemaExport().setHaltOnError( true ).drop( EnumSet.of( TargetType.DATABASE ), metadata );
	}

	@MappedSuperclass
	static class Owner {
		@Id Long id;
		@Column(length = 50) String name;
	}

	@MappedSuperclass
	static class Target {
		@Id Long id;
		@Column(name = "lang_code", length = 10) String language;
		@Column(name = "target_text", length = 50) String text;
	}

	@Entity
	@Table(name = "candidate_owner")
	static class LabelOwner extends Owner {
		@Column(name = "label_id") Long label;
	}

	@Entity
	@Table(name = "candidate_translation", uniqueConstraints = @UniqueConstraint(columnNames = {"label_id", "lang_code"}))
	static class Translation extends Target {
		@Column(name = "label_id") Long label;
	}

	@Entity
	@Table(name = "candidate_key_owner")
	static class KeyOwner extends Owner {
		@Column(length = 10) String tenant;
		@Column(name = "label_code", length = 30) String label;
	}

	@Entity
	@Table(name = "candidate_key_target", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant", "label_code", "lang_code"}))
	static class KeyTarget extends Target {
		@Column(length = 10) String tenant;
		@Column(name = "label_code", length = 30) String label;
	}

	@Entity
	@Table(name = "candidate_composite_owner")
	static class CompositeOwner extends Owner {
		@Column(name = "label_code", length = 30) String label;
	}

	@Entity
	@Table(name = "candidate_composite_target", uniqueConstraints = @UniqueConstraint(columnNames = {"label_code", "lang_code"}))
	static class CompositeTarget {
		@EmbeddedId RestrictedToOneCandidateCompositeTest.Key id;
		@Column(name = "label_code", length = 30) String label;
		@Column(name = "lang_code", length = 10) String language;
		@Column(name = "target_text", length = 50) String text;
		int active;
	}

	@MappedSuperclass
	static class Link {
		@Id @Column(name = "owner_id") Long owner;
		@Column(name = "label_code", length = 30) String label;
		int enabled;
	}

	@Entity
	@Table(name = "candidate_composite_link")
	static class CompositeLink extends Link {}

	@Entity
	@Table(name = "candidate_composite_english")
	static class CompositeEnglish extends Link {}

	@Entity
	@Table(name = "entity_candidate_owner")
	static class EntityOwner extends Owner {
		@Column(name = "label_code", length = 30) String label;
	}

	@Entity
	@Table(name = "entity_candidate_target", uniqueConstraints = @UniqueConstraint(columnNames = {"label_code", "lang_code"}))
	static class EntityTarget extends Target {
		@Column(name = "label_code", length = 30) String label;
		@Column(name = "next_code", length = 30) String next;
	}
}
