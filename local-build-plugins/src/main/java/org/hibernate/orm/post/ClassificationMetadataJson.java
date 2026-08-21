/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;

/// Reads and writes the supported `classifications.json` contract.
///
/// @author Steve Ebersole
public final class ClassificationMetadataJson {
	/// Writes deterministic JSON in stable model order.
	public String write(ClassificationMetadata metadata) {
		final Map<String, Object> root = new LinkedHashMap<>();
		root.put( "schema", ClassificationMetadata.SCHEMA );
		root.put( "schemaVersion", ClassificationMetadata.SCHEMA_VERSION );
		root.put( "hibernateVersion", metadata.getHibernateVersion() );
		root.put( "sourceVersion", metadata.getSourceVersion() );

		final List<Map<String, Object>> elements = new ArrayList<>();
		for ( ClassificationModel.Element element : metadata.getModel().getElements() ) {
			elements.add( element( element ) );
		}
		root.put( "elements", elements );

		try ( Jsonb jsonb = JsonbBuilder.create( jsonConfig() ) ) {
			return jsonb.toJson( root ) + '\n';
		}
		catch (Exception e) {
			throw new IllegalStateException( "Unable to write classification metadata", e );
		}
	}

	/// Reads and validates a supported classification document.
	public ClassificationMetadata read(String json) {
		return read( new StringReader( json ) );
	}

	/// Reads either an uncompressed JSON document or its gzip encoding.
	public ClassificationMetadata read(Path path) {
		try ( InputStream fileStream = Files.newInputStream( path );
				BufferedInputStream bufferedStream = new BufferedInputStream( fileStream ) ) {
			bufferedStream.mark( 2 );
			final int first = bufferedStream.read();
			final int second = bufferedStream.read();
			bufferedStream.reset();
			final InputStream contents = first == 0x1f && second == 0x8b
					? new GZIPInputStream( bufferedStream )
					: bufferedStream;
			return read( new InputStreamReader( contents, StandardCharsets.UTF_8 ) );
		}
		catch (IOException e) {
			throw new IllegalArgumentException( "Unable to read classification metadata " + path, e );
		}
	}

	/// Encodes an already serialized document using deterministic gzip bytes.
	public byte[] gzip(String json) {
		try ( ByteArrayOutputStream bytes = new ByteArrayOutputStream();
				GZIPOutputStream gzip = new GZIPOutputStream( bytes ) ) {
			gzip.write( json.getBytes( StandardCharsets.UTF_8 ) );
			gzip.finish();
			return bytes.toByteArray();
		}
		catch (IOException e) {
			throw new IllegalStateException( "Unable to gzip classification metadata", e );
		}
	}

	@SuppressWarnings("unchecked")
	private ClassificationMetadata read(Reader reader) {
		final Map<String, Object> root;
		try ( Jsonb jsonb = JsonbBuilder.create( jsonConfig() ) ) {
			root = jsonb.fromJson( reader, Map.class );
		}
		catch (Exception e) {
			throw new IllegalArgumentException( "Unable to read classification metadata", e );
		}
		if ( root == null ) {
			throw new IllegalArgumentException( "Classification metadata document is empty" );
		}

		final String schema = string( root, "schema" );
		if ( !ClassificationMetadata.SCHEMA.equals( schema ) ) {
			throw new UnsupportedSchemaException( "Unsupported classification schema: " + schema );
		}
		final int schemaVersion = integer( root, "schemaVersion" );
		if ( schemaVersion != ClassificationMetadata.SCHEMA_VERSION ) {
			throw new UnsupportedSchemaException( "Unsupported classification schema version: " + schemaVersion );
		}

		final ClassificationModel.Builder model = ClassificationModel.builder();
		final List<Map<String, Object>> elements = maps( root, "elements" );
		for ( Map<String, Object> element : elements ) {
			declare( model, element );
		}
		for ( Map<String, Object> element : elements ) {
			populate( model, element );
		}

		final ClassificationModel classificationModel = model.build();
		validateEffectiveValues( classificationModel, elements );
		return new ClassificationMetadata(
				string( root, "hibernateVersion" ),
				string( root, "sourceVersion" ),
				classificationModel
		);
	}

	private static JsonbConfig jsonConfig() {
		return new JsonbConfig()
				.withNullValues( true );
	}

	private static Map<String, Object> element(ClassificationModel.Element element) {
		final Map<String, Object> json = new LinkedHashMap<>();
		json.put( "id", element.getId() );
		json.put( "kind", element.getKind().name() );
		json.put( "owner", element.getOwnerId() );
		json.put( "declaringPackage", element.getDeclaringPackage() );
		json.put( "signature", element.getSignature() );
		json.put( "classificationStatus", element.getClassificationStatus().name() );
		json.put( "category", element.getCategory() == null ? null : element.getCategory().name() );
		json.put( "declaredSpiRoles", names( element.getDeclaredRoles() ) );
		json.put( "spiRoles", names( element.getEffectiveRoles() ) );
		json.put( "classificationOrigins", classificationOrigins( element.getClassificationOrigins() ) );
		json.put( "structure", structure( element.getStructure() ) );
		json.put( "lifecycle", lifecycle( element.getLifecycle() ) );
		json.put( "artifact", element.getArtifact() );
		json.put( "references", references( element.getReferences() ) );
		return json;
	}

	private static List<Map<String, Object>> classificationOrigins(
			Collection<ClassificationModel.ClassificationOrigin> origins) {
		final List<Map<String, Object>> result = new ArrayList<>();
		for ( ClassificationModel.ClassificationOrigin origin : origins ) {
			final Map<String, Object> json = new LinkedHashMap<>();
			json.put( "category", origin.getCategory().name() );
			json.put( "kind", origin.getKind().name() );
			json.put( "sourceElementId", origin.getSourceElementId() );
			json.put( "spiRoles", names( origin.getRoles() ) );
			result.add( json );
		}
		return result;
	}

	private static Map<String, Object> structure(ClassificationModel.Structure structure) {
		final Map<String, Object> json = new LinkedHashMap<>();
		json.put( "known", structure.isKnown() );
		json.put( "modifiers", structure.getModifiers() );
		json.put( "interfaceType", structure.isInterfaceType() );
		json.put( "declaringTypeFinal", structure.isDeclaringTypeFinal() );
		return json;
	}

	private static Map<String, Object> lifecycle(ClassificationModel.Lifecycle lifecycle) {
		final Map<String, Object> json = new LinkedHashMap<>();
		json.put( "incubating", lifecycle.isIncubating() );
		json.put( "deprecated", lifecycle.isDeprecated() );
		json.put( "forRemoval", lifecycle.isForRemoval() );
		json.put( "removal", lifecycle.isRemoval() );
		final List<Map<String, Object>> origins = new ArrayList<>();
		for ( ClassificationModel.LifecycleOrigin origin : lifecycle.getOrigins() ) {
			final Map<String, Object> jsonOrigin = new LinkedHashMap<>();
			jsonOrigin.put( "state", origin.getState().name() );
			jsonOrigin.put( "kind", origin.getKind().name() );
			jsonOrigin.put( "sourceElementId", origin.getSourceElementId() );
			origins.add( jsonOrigin );
		}
		json.put( "origins", origins );
		return json;
	}

	private static List<Map<String, Object>> references(Collection<ClassificationModel.Reference> references) {
		final List<Map<String, Object>> result = new ArrayList<>();
		for ( ClassificationModel.Reference reference : references ) {
			final Map<String, Object> json = new LinkedHashMap<>();
			json.put( "kind", reference.getKind().name() );
			json.put( "target", reference.getTargetElementId() );
			json.put( "targetScope", reference.getTarget().name() );
			result.add( json );
		}
		return result;
	}

	private static List<String> names(Collection<? extends Enum<?>> values) {
		final List<String> names = new ArrayList<>();
		for ( Enum<?> value : values ) {
			names.add( value.name() );
		}
		return names;
	}

	private static void declare(ClassificationModel.Builder model, Map<String, Object> element) {
		final Map<String, Object> structure = map( element, "structure" );
		model.declaration(
				string( element, "id" ),
				enumValue( ClassificationModel.ElementKind.class, element, "kind" ),
				nullableString( element, "owner" ),
				string( element, "declaringPackage" ),
				string( element, "signature" ),
				booleanValue( structure, "known" )
						? new ClassificationModel.Structure(
								integer( structure, "modifiers" ),
								booleanValue( structure, "interfaceType" ),
								booleanValue( structure, "declaringTypeFinal" )
						)
						: ClassificationModel.Structure.UNKNOWN,
				string( element, "artifact" )
		);
	}

	private static void populate(ClassificationModel.Builder model, Map<String, Object> element) {
		final String elementId = string( element, "id" );
		final Collection<ClassificationModel.Role> declaredRoles = enumValues(
				ClassificationModel.Role.class,
				element,
				"declaredSpiRoles"
		);
		for ( Map<String, Object> origin : maps( element, "classificationOrigins" ) ) {
			model.addClassificationOrigin(
					elementId,
					new ClassificationModel.ClassificationOrigin(
							enumValue( ClassificationModel.Category.class, origin, "category" ),
							enumValue( ClassificationModel.OriginKind.class, origin, "kind" ),
							string( origin, "sourceElementId" ),
							enumValues( ClassificationModel.Role.class, origin, "spiRoles" )
					),
					declaredRoles
			);
		}

		final Map<String, Object> lifecycle = map( element, "lifecycle" );
		for ( Map<String, Object> origin : maps( lifecycle, "origins" ) ) {
			model.addLifecycleOrigin(
					elementId,
					new ClassificationModel.LifecycleOrigin(
							enumValue( ClassificationModel.LifecycleState.class, origin, "state" ),
							enumValue( ClassificationModel.LifecycleOriginKind.class, origin, "kind" ),
							string( origin, "sourceElementId" )
					)
			);
		}

		for ( Map<String, Object> reference : maps( element, "references" ) ) {
			model.addReference(
					elementId,
					new ClassificationModel.Reference(
							enumValue( ClassificationModel.ReferenceKind.class, reference, "kind" ),
							string( reference, "target" ),
							enumValue( ClassificationModel.ReferenceTarget.class, reference, "targetScope" )
					)
			);
		}
	}

	private static void validateEffectiveValues(
			ClassificationModel model,
			List<Map<String, Object>> serializedElements) {
		for ( Map<String, Object> serialized : serializedElements ) {
			final String elementId = string( serialized, "id" );
			final ClassificationModel.Element element = model.getElement( elementId );
			if ( element == null ) {
				throw new IllegalArgumentException( "Missing reconstructed element " + elementId );
			}
			final ClassificationModel.ClassificationStatus classificationStatus = enumValue(
					ClassificationModel.ClassificationStatus.class,
					serialized,
					"classificationStatus"
			);
			if ( element.getClassificationStatus() != classificationStatus ) {
				throw new IllegalArgumentException( "Classification status does not match its origins for " + elementId );
			}
			final ClassificationModel.Category category = nullableEnumValue(
					ClassificationModel.Category.class,
					serialized,
					"category"
			);
			if ( element.getCategory() != category ) {
				throw new IllegalArgumentException( "Category does not match its origins for " + elementId );
			}
			final Collection<ClassificationModel.Role> roles = enumValues(
					ClassificationModel.Role.class,
					serialized,
					"spiRoles"
			);
			if ( !element.getEffectiveRoles().equals( roles ) ) {
				throw new IllegalArgumentException( "SPI roles do not match their origins for " + elementId );
			}
			final Map<String, Object> lifecycle = map( serialized, "lifecycle" );
			if ( element.getLifecycle().isIncubating() != booleanValue( lifecycle, "incubating" )
					|| element.getLifecycle().isDeprecated() != booleanValue( lifecycle, "deprecated" )
					|| element.getLifecycle().isForRemoval() != booleanValue( lifecycle, "forRemoval" )
					|| element.getLifecycle().isRemoval() != booleanValue( lifecycle, "removal" ) ) {
				throw new IllegalArgumentException( "Lifecycle values do not match their origins for " + elementId );
			}
		}
	}

	private static String string(Map<String, Object> values, String name) {
		final String value = nullableString( values, name );
		if ( value == null ) {
			throw new IllegalArgumentException( "Missing string property: " + name );
		}
		return value;
	}

	private static String nullableString(Map<String, Object> values, String name) {
		final Object value = values.get( name );
		if ( value == null ) {
			return null;
		}
		if ( !(value instanceof String) ) {
			throw new IllegalArgumentException( "Property is not a string: " + name );
		}
		return (String) value;
	}

	private static int integer(Map<String, Object> values, String name) {
		final Object value = values.get( name );
		if ( !(value instanceof Number) ) {
			throw new IllegalArgumentException( "Property is not a number: " + name );
		}
		return ((Number) value).intValue();
	}

	private static boolean booleanValue(Map<String, Object> values, String name) {
		final Object value = values.get( name );
		if ( !(value instanceof Boolean) ) {
			throw new IllegalArgumentException( "Property is not a boolean: " + name );
		}
		return (Boolean) value;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> map(Map<String, Object> values, String name) {
		final Object value = values.get( name );
		if ( !(value instanceof Map) ) {
			throw new IllegalArgumentException( "Property is not an object: " + name );
		}
		return (Map<String, Object>) value;
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> maps(Map<String, Object> values, String name) {
		final Object value = values.get( name );
		if ( !(value instanceof List) ) {
			throw new IllegalArgumentException( "Property is not an array: " + name );
		}
		return (List<Map<String, Object>>) value;
	}

	private static <E extends Enum<E>> E enumValue(
			Class<E> enumType,
			Map<String, Object> values,
			String name) {
		try {
			return Enum.valueOf( enumType, string( values, name ) );
		}
		catch (IllegalArgumentException e) {
			throw new IllegalArgumentException( "Invalid " + name + " value", e );
		}
	}

	private static <E extends Enum<E>> E nullableEnumValue(
			Class<E> enumType,
			Map<String, Object> values,
			String name) {
		if ( !values.containsKey( name ) ) {
			throw new IllegalArgumentException( "Missing property: " + name );
		}
		final String value = nullableString( values, name );
		if ( value == null ) {
			return null;
		}
		try {
			return Enum.valueOf( enumType, value );
		}
		catch (IllegalArgumentException e) {
			throw new IllegalArgumentException( "Unknown " + enumType.getSimpleName() + " value: " + value, e );
		}
	}

	private static <E extends Enum<E>> Collection<E> enumValues(
			Class<E> enumType,
			Map<String, Object> values,
			String name) {
		final Object value = values.get( name );
		if ( !(value instanceof List) ) {
			throw new IllegalArgumentException( "Property is not an array: " + name );
		}
		final List<?> serialized = (List<?>) value;
		if ( serialized.isEmpty() ) {
			return Collections.emptySet();
		}
		final EnumSet<E> result = EnumSet.noneOf( enumType );
		for ( Object item : serialized ) {
			if ( !(item instanceof String) ) {
				throw new IllegalArgumentException( "Non-string value in " + name );
			}
			result.add( Enum.valueOf( enumType, (String) item ) );
		}
		return Collections.unmodifiableSet( result );
	}

	/// Indicates that a document uses a schema name or version unsupported by
	/// this reader.
	public static final class UnsupportedSchemaException extends IllegalArgumentException {
		public UnsupportedSchemaException(String message) {
			super( message );
		}
	}
}
