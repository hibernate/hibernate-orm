/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.serial.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.InputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import org.hibernate.Version;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.internal.MetadataImpl;
import org.hibernate.boot.models.internal.ClassLoaderServiceLoading;
import org.hibernate.boot.models.internal.ModelsHelper;
import org.hibernate.boot.mapping.internal.context.MappingResolutionState;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.mapping.internal.model.BootBindingModel;
import org.hibernate.boot.pipeline.internal.ResolvedMapping;
import org.hibernate.boot.pipeline.internal.ResolvedMappingImplementor;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.serial.MetadataArchive;
import org.hibernate.boot.serial.MetadataSerializationException;
import org.hibernate.boot.serial.RestoredMetadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.ClassLoaderAccess;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.BasicValue;
import org.hibernate.models.serial.spi.ModelsArchive;
import org.hibernate.models.serial.spi.ModelsArchives;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.Service;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.spi.TypeConfiguration;

/// Standard [MetadataArchive] implementation.
///
/// @since 9.0
/// @author Steve Ebersole
public final class MetadataArchiveImpl implements MetadataArchive {
	static final int MAGIC = 0x484f524d; // HORM
	static final int FORMAT_VERSION = 1;
	static final int MAX_PAYLOAD_SIZE = 256 * 1024 * 1024;
	static final String ORM_VERSION = Version.getVersionString();

	private final ModelsArchive modelsArchive;
	private final byte[] payload;

	MetadataArchiveImpl(ModelsArchive modelsArchive, byte[] payload) {
		this.modelsArchive = modelsArchive;
		this.payload = payload;
	}

	public static MetadataArchiveImpl from(Metadata metadata) {
		if ( !( metadata instanceof ResolvedMappingImplementor resolvedMappingImplementor )
				|| !( resolvedMappingImplementor.getResolvedMapping().metadata() instanceof MetadataImpl metadataImpl ) ) {
			throw new IllegalArgumentException(
					"Metadata serialization requires a resolved mapping; bare MetadataImpl is not factory-ready"
			);
		}
		final ResolvedMapping resolvedMapping = resolvedMappingImplementor.getResolvedMapping();
		if ( resolvedMapping.mappingResolutionDetailsCollector() == null ) {
			throw new IllegalStateException(
					"Metadata serialization was not enabled while the mapping was built; set '"
							+ org.hibernate.cfg.MappingSettings.METADATA_SERIALIZATION_ENABLED + "' to true"
			);
		}
		final var mappingResolutionSnapshot = resolvedMapping.mappingResolutionDetailsCollector().freeze( metadataImpl );
		if ( !( resolvedMapping.runtimeMappingHandoff() instanceof RuntimeMappingHandoffSnapshot handoffSnapshot ) ) {
			throw new IllegalArgumentException( "Resolved mapping does not expose a serializable runtime handoff snapshot" );
		}
		if ( !metadataImpl.getSqlFunctionMap().isEmpty() ) {
			throw new MetadataSerializationException(
					"ORM metadata with explicitly contributed SQL functions cannot be serialized; "
							+ "contribute the functions through FunctionContributor so they can be rebuilt during restoration",
					new IllegalStateException( "SQL function descriptors do not define a declarative serial form" )
			);
		}

		try {
			final var archiveWriter = ModelsArchives.createWriter( true );
			final var payloadBytes = new ByteArrayOutputStream();
			try (ObjectOutputStream output = ModelsArchives.createObjectOutputStream( payloadBytes, archiveWriter )) {
				output.writeObject( new SerializedResolvedMapping(
						MetadataState.from( metadataImpl ),
						mappingResolutionSnapshot,
						handoffSnapshot
				) );
			}
			return new MetadataArchiveImpl( archiveWriter.finish(), payloadBytes.toByteArray() );
		}
		catch (IOException e) {
			throw new MetadataSerializationException( "Could not serialize ORM metadata payload", e );
		}
	}

	@Override
	public RestoredMetadata restore(StandardServiceRegistry serviceRegistry) {
		if ( serviceRegistry == null ) {
			throw new IllegalArgumentException( "StandardServiceRegistry cannot be null" );
		}
		checkInitialized();
		try {
			final var classLoading = new ClassLoaderServiceLoading(
					serviceRegistry.requireService( ClassLoaderService.class )
			);
			final var restoredModels = modelsArchive.restore( classLoading, ModelsHelper::preFillRegistries );
			try (var input = ModelsArchives.createObjectInputStream(
					new ByteArrayInputStream( payload ),
					restoredModels )) {
				input.setObjectInputFilter( MetadataArchiveImpl::checkPayloadType );
				final Object root = input.readObject();
			if ( !( root instanceof SerializedResolvedMapping state ) ) {
				throw new MetadataSerializationException( "ORM metadata payload has an unexpected root: "
						+ ( root == null ? "null" : root.getClass().getName() ),
						new IllegalStateException( "Unexpected ORM metadata payload root" ) );
			}
			final MetadataImpl metadata = state.metadataState().restore(
					serviceRegistry,
					restoredModels.getModelsContext()
			);
			final var buildingContext = metadata.getTypeConfiguration().getMetadataBuildingContext();
			final var resolutionState = new MappingResolutionState(
					metadata,
					metadata.getDatabase(),
					metadata.getMappingResolutionOptions(),
					buildingContext.getTypeDefinitionRegistry()
			);
			state.mappingResolutionSnapshot().restore(
					metadata,
					buildingContext.getServiceComponents(),
					resolutionState,
					buildingContext
			);
			final RuntimeMappingHandoffSnapshot runtimeHandoff =
					state.runtimeMappingHandoffSnapshot().resolveAgainst( metadata );
			final var restoredMetadata = new ResolvedMappingImplementor( new ResolvedMapping(
					metadata,
					null,
					runtimeHandoff
			) );
			return new RestoredMetadataImpl( restoredMetadata );
			}
		}
		catch (IOException | ClassNotFoundException e) {
			throw new MetadataSerializationException( "Could not restore ORM metadata payload", e );
		}
		catch (MetadataSerializationException e) {
			throw e;
		}
		catch (RuntimeException e) {
			throw new MetadataSerializationException( "Could not restore ORM metadata archive", e );
		}
	}

	private static ObjectInputFilter.Status checkPayloadType(ObjectInputFilter.FilterInfo filterInfo) {
		final Class<?> serialClass = filterInfo.serialClass();
		if ( serialClass == null ) {
			return ObjectInputFilter.Status.UNDECIDED;
		}
		final Class<?> payloadClass = serialClass.isArray() ? serialClass.getComponentType() : serialClass;
		return BootBindingModel.class.isAssignableFrom( payloadClass )
				|| BindingState.class.isAssignableFrom( payloadClass )
				|| BasicValue.Resolution.class.isAssignableFrom( payloadClass )
				|| Service.class.isAssignableFrom( payloadClass )
				|| ServiceRegistry.class.isAssignableFrom( payloadClass )
				|| BootstrapContext.class.isAssignableFrom( payloadClass )
				|| MetadataBuildingContext.class.isAssignableFrom( payloadClass )
				|| ClassLoaderAccess.class.isAssignableFrom( payloadClass )
				|| ManagedBeanRegistry.class.isAssignableFrom( payloadClass )
				|| ModelsContext.class.isAssignableFrom( payloadClass )
				|| TypeConfiguration.class.isAssignableFrom( payloadClass )
				? ObjectInputFilter.Status.REJECTED
				: ObjectInputFilter.Status.UNDECIDED;
	}

	@Override
	public void writeTo(OutputStream stream) {
		if ( stream == null ) {
			throw new IllegalArgumentException( "OutputStream cannot be null" );
		}
		checkInitialized();
		try {
			final ObjectOutputStream output = new ObjectOutputStream( stream );
			output.writeInt( MAGIC );
			output.writeInt( FORMAT_VERSION );
			output.writeUTF( ORM_VERSION );
			output.writeObject( modelsArchive );
			output.writeInt( payload.length );
			output.write( payload );
			output.flush();
		}
		catch (IOException e) {
			throw new MetadataSerializationException( "Could not write ORM metadata archive", e );
		}
	}

	public static MetadataArchiveImpl readFrom(InputStream stream) {
		if ( stream == null ) {
			throw new IllegalArgumentException( "InputStream cannot be null" );
		}
		try {
			final ObjectInputStream input = new ObjectInputStream( stream );
			if ( input.readInt() != MAGIC ) {
				throw new InvalidObjectException( "Invalid ORM metadata archive header" );
			}
			final int version = input.readInt();
			if ( version != FORMAT_VERSION ) {
				throw new InvalidObjectException( "Unsupported ORM metadata archive format version: " + version );
			}
			final String producerVersion = input.readUTF();
			if ( !ORM_VERSION.equals( producerVersion ) ) {
				throw new InvalidObjectException(
						"ORM metadata archive was produced by Hibernate ORM " + producerVersion
								+ ", but the consumer is Hibernate ORM " + ORM_VERSION
				);
			}
			final Object archive = input.readObject();
			if ( !( archive instanceof ModelsArchive readModelsArchive ) ) {
				throw new InvalidObjectException( "ORM metadata archive does not contain a ModelsArchive" );
			}
			final int payloadLength = input.readInt();
			if ( payloadLength < 0 || payloadLength > MAX_PAYLOAD_SIZE ) {
				throw new InvalidObjectException( "Invalid ORM metadata payload length: " + payloadLength );
			}
			final byte[] payload = new byte[payloadLength];
			input.readFully( payload );
			return new MetadataArchiveImpl( readModelsArchive, payload );
		}
		catch (IOException | ClassNotFoundException e) {
			throw new MetadataSerializationException( "Could not read ORM metadata archive", e );
		}
	}

	private void checkInitialized() {
		if ( modelsArchive == null || payload == null ) {
			throw new IllegalStateException( "ORM metadata serial form is not initialized" );
		}
	}
}
