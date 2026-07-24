/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.serial.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InvalidObjectException;
import java.io.ObjectOutputStream;

import org.hibernate.boot.serial.MetadataSerializationException;
import org.hibernate.models.serial.spi.ModelsArchive;
import org.hibernate.models.serial.spi.ModelsArchives;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ServiceRegistry
class MetadataArchiveFormatTests {
	@Test
	void nullStreamsAreRejected() {
		assertThatThrownBy( () -> MetadataArchiveImpl.readFrom( null ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "InputStream" );
		final var archive = new MetadataArchiveImpl( modelsArchive(), new byte[0] );
		assertThatThrownBy( () -> archive.writeTo( null ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "OutputStream" );
		assertThatThrownBy( () -> archive.restore( null ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "StandardServiceRegistry" );
	}

	@Test
	void invalidHeaderIsRejected() throws Exception {
		assertInvalidArchive(
				archive( 0, MetadataArchiveImpl.FORMAT_VERSION, MetadataArchiveImpl.ORM_VERSION, modelsArchive(), 0 ),
				"header"
		);
	}

	@Test
	void unknownFormatVersionIsRejected() throws Exception {
		assertInvalidArchive(
				archive(
						MetadataArchiveImpl.MAGIC,
						MetadataArchiveImpl.FORMAT_VERSION + 1,
						MetadataArchiveImpl.ORM_VERSION,
						modelsArchive(),
						0
				),
				"format version"
		);
	}

	@Test
	void differentOrmVersionIsRejected() throws Exception {
		assertInvalidArchive(
				archive(
						MetadataArchiveImpl.MAGIC,
						MetadataArchiveImpl.FORMAT_VERSION,
						"other-version",
						modelsArchive(),
						0
				),
				"other-version"
		);
	}

	@Test
	void missingModelsArchiveIsRejected() throws Exception {
		assertInvalidArchive(
				archive(
						MetadataArchiveImpl.MAGIC,
						MetadataArchiveImpl.FORMAT_VERSION,
						MetadataArchiveImpl.ORM_VERSION,
						"not a models archive",
						0
				),
				"ModelsArchive"
		);
	}

	@Test
	void invalidPayloadLengthsAreRejected() throws Exception {
		assertInvalidArchive(
				archive(
						MetadataArchiveImpl.MAGIC,
						MetadataArchiveImpl.FORMAT_VERSION,
						MetadataArchiveImpl.ORM_VERSION,
						modelsArchive(),
						-1
				),
				"payload length"
		);
		assertInvalidArchive(
				archive(
						MetadataArchiveImpl.MAGIC,
						MetadataArchiveImpl.FORMAT_VERSION,
						MetadataArchiveImpl.ORM_VERSION,
						modelsArchive(),
						MetadataArchiveImpl.MAX_PAYLOAD_SIZE + 1
				),
				"payload length"
		);
	}

	@Test
	void truncatedPayloadIsRejected() throws Exception {
		assertThatThrownBy( () -> MetadataArchiveImpl.readFrom( new ByteArrayInputStream(
				archive(
						MetadataArchiveImpl.MAGIC,
						MetadataArchiveImpl.FORMAT_VERSION,
						MetadataArchiveImpl.ORM_VERSION,
						modelsArchive(),
						4,
						(byte) 1,
						(byte) 2
				)
		) ) )
				.isInstanceOf( MetadataSerializationException.class )
				.hasMessageContaining( "read ORM metadata archive" );
	}

	@Test
	void corruptPayloadFailureIsNormalized(ServiceRegistryScope scope) {
		assertThatThrownBy( () -> new MetadataArchiveImpl( modelsArchive(), new byte[0] )
				.restore( scope.getRegistry() ) )
				.isInstanceOf( MetadataSerializationException.class )
				.hasMessageContaining( "restore ORM metadata payload" );
	}

	@Test
	void unexpectedPayloadRootIsRejected(ServiceRegistryScope scope) throws Exception {
		final var archiveWriter = ModelsArchives.createWriter( true );
		final var payload = new ByteArrayOutputStream();
		try ( var output = ModelsArchives.createObjectOutputStream( payload, archiveWriter ) ) {
			output.writeObject( "unexpected" );
		}
		final var archive = new MetadataArchiveImpl( archiveWriter.finish(), payload.toByteArray() );

		assertThatThrownBy( () -> archive.restore( scope.getRegistry() ) )
				.isInstanceOf( MetadataSerializationException.class )
				.hasMessageContaining( "unexpected root" );
	}

	private static ModelsArchive modelsArchive() {
		return ModelsArchives.createWriter( true ).finish();
	}

	private static byte[] archive(
			int magic,
			int formatVersion,
			String ormVersion,
			Object modelsArchive,
			int payloadLength,
			byte... payload) throws Exception {
		final var bytes = new ByteArrayOutputStream();
		final var output = new ObjectOutputStream( bytes );
		output.writeInt( magic );
		output.writeInt( formatVersion );
		output.writeUTF( ormVersion );
		output.writeObject( modelsArchive );
		output.writeInt( payloadLength );
		output.write( payload );
		output.flush();
		return bytes.toByteArray();
	}

	private static void assertInvalidArchive(byte[] bytes, String message) {
		assertThatThrownBy( () -> MetadataArchiveImpl.readFrom( new ByteArrayInputStream( bytes ) ) )
				.isInstanceOf( MetadataSerializationException.class )
				.hasCauseInstanceOf( InvalidObjectException.class )
				.satisfies( exception -> assertThat( exception.getCause() ).hasMessageContaining( message ) );
	}
}
