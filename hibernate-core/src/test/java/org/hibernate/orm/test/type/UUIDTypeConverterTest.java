/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.type;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.community.dialect.InformixDialect;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.hibernate.testing.util.uuid.SafeRandomUUIDGenerator.safeRandomUUID;

@DomainModel(
		annotatedClasses = {
				UUIDTypeConverterTest.Image.class,
				UUIDTypeConverterTest.MarbleBox.class,
				UUIDTypeConverterTest.Marble.class
		}
)
@SessionFactory
@TestForIssue(jiraKey = "HHH-15417")
@SkipForDialect(dialectClass = InformixDialect.class, reason = "Informix does not support unique / primary constraints on binary columns")
public class UUIDTypeConverterTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "delete from Image" ).executeUpdate();
					session.createMutationQuery( "delete from MarbleBox" ).executeUpdate();
					session.createMutationQuery( "delete from Marble" ).executeUpdate();
				}
		);
	}

	@Test
	public void testMerge(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.merge( new Image() )
		);
	}

	@Test
	public void testMergeAndFlushDetached(SessionFactoryScope scope) {
		Image image = scope.fromTransaction(
				session ->
						session.merge( new Image() )
		);
		scope.inTransaction(
				session -> {
					image.setThumbId( safeRandomUUID() );
					session.merge( image );
					session.flush();
				}
		);
	}

	@Test
	public void testMergeAndFlush(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Image image = session.merge( new Image() );
					image.setThumbId( safeRandomUUID() );
					session.merge( image );
					session.flush();
				}
		);
	}

	@Test
	public void testMerge2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					MarbleBox marbleBox = new MarbleBox( List.of( new Marble() ) );

					MarbleBox saved = session.merge( marbleBox );
					saved.getMarbles().get( 0 ).setMaterialId( safeRandomUUID() );
					session.merge( saved );
				}
		);
	}

	@Test
	public void testMergeDetached(SessionFactoryScope scope) {
		MarbleBox marbleBox = scope.fromTransaction(
				session -> {

					MarbleBox saved = session.merge( new MarbleBox( List.of( new Marble() ) ) );

					return saved;
				}
		);

		scope.inTransaction(
				session -> {
					marbleBox.getMarbles().get( 0 ).setMaterialId( safeRandomUUID() );
					session.merge( marbleBox );
				}
		);
	}

	@MappedSuperclass
	public static class Id {
		@Column(unique = true, length = 16, nullable = false)
		@jakarta.persistence.Id
		@Convert(converter = UuidBase64TypeConverter.class)
		private UUID id = safeRandomUUID();
	}

	@Entity(name = "Image")
	@Table(name = "TEST_IMAGE")
	public static class Image extends Id {

		@Column(unique = true, length = 16, nullable = false)
		@Convert(converter = UuidBase64TypeConverter.class)
		private UUID thumbId = safeRandomUUID();

		private int position;

		public void setThumbId(UUID thumbId) {
			this.thumbId = thumbId;
		}

		public void setPosition(int position) {
			this.position = position;
		}

	}

	@Entity(name = "MarbleBox")
	public static class MarbleBox extends Id {
		@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
		private List<Marble> marbles = new ArrayList<>();

		private String description;

		public MarbleBox() {
		}

		public MarbleBox(List<Marble> marbles) {
			this.marbles = marbles;
		}

		public List<Marble> getMarbles() {
			return marbles;
		}
	}

	@Entity(name = "Marble")
	public static class Marble extends Id {
		@Column(length = 16)
		@Convert(converter = UuidBase64TypeConverter.class)
		private UUID materialId;

		private String color;

		public void setMaterialId(UUID materialId) {
			this.materialId = materialId;
		}
	}

	public static class UuidBase64TypeConverter implements AttributeConverter<UUID, byte[]> {
		@Override
		public byte[] convertToDatabaseColumn(UUID attribute) {
			return toBytes( attribute );
		}

		@Override
		public UUID convertToEntityAttribute(byte[] dbData) {
			return toUuid( dbData );
		}

		private UUID toUuid(byte[] bytes) {
			if ( bytes == null || bytes.length < 16 ) {
				return null;
			}
			long mostSignificantBits = getMostSignificantBits( bytes );
			long leastSignificantBits = getLeastSignificantBits( bytes );
			return new UUID( mostSignificantBits, leastSignificantBits );
		}

		private long getMostSignificantBits(byte[] bytes) {
			byte[] b = new byte[8];
			for ( int i = 0; i < 8; i++ ) {
				b[i] = bytes[i];
			}
			return toLong( b );
		}

		private long getLeastSignificantBits(byte[] bytes) {
			byte[] b = new byte[8];
			int j = 0;
			for ( int i = 8; i < 16; i++ ) {
				b[j++] = bytes[i];
			}
			return toLong( b );
		}

		private long toLong(byte[] bytes) {
			return ByteBuffer.wrap( bytes ).getLong();
		}

		private byte[] toBytes(UUID uuid) {
			if ( uuid == null ) {
				return new byte[] {};
			}
			ByteBuffer bb = ByteBuffer.wrap( new byte[16] );
			bb.putLong( uuid.getMostSignificantBits() );
			bb.putLong( uuid.getLeastSignificantBits() );
			return bb.array();
		}
	}

}
