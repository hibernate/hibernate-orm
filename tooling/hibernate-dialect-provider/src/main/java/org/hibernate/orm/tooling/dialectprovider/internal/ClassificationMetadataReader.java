/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.dialectprovider.internal;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

/// Reads the stable, provider-consumable classification metadata schema.
///
/// @author Steve Ebersole
public final class ClassificationMetadataReader {
	public ClassificationMetadata read(Path path) {
		try ( InputStream input = Files.newInputStream( path );
				BufferedInputStream buffered = new BufferedInputStream( input ) ) {
			buffered.mark( 2 );
			final int first = buffered.read();
			final int second = buffered.read();
			buffered.reset();
			final InputStream contents = first == 0x1f && second == 0x8b
					? new GZIPInputStream( buffered )
					: buffered;
			return read( new InputStreamReader( contents, StandardCharsets.UTF_8 ) );
		}
		catch (IOException e) {
			throw new IllegalArgumentException( "Unable to read classification metadata " + path, e );
		}
	}

	@SuppressWarnings("unchecked")
	private ClassificationMetadata read(Reader reader) {
		final Map<String, Object> root;
		try ( Jsonb jsonb = JsonbBuilder.create() ) {
			root = jsonb.fromJson( reader, Map.class );
		}
		catch (Exception e) {
			throw new IllegalArgumentException( "Malformed classification metadata", e );
		}
		if ( root == null ) {
			throw new IllegalArgumentException( "Classification metadata document is empty" );
		}
		if ( !ClassificationMetadata.SCHEMA.equals( string( root, "schema" ) ) ) {
			throw new IllegalArgumentException( "Unsupported classification metadata schema" );
		}
		final Number schemaVersion = number( root, "schemaVersion" );
		if ( schemaVersion.intValue() != ClassificationMetadata.SCHEMA_VERSION ) {
			throw new IllegalArgumentException(
					"Unsupported classification metadata schema version " + schemaVersion.intValue()
			);
		}

		final Map<String, ClassificationMetadata.Element> elements = new LinkedHashMap<>();
		for ( Map<String, Object> raw : maps( root, "elements" ) ) {
			final String id = string( raw, "id" );
			final ClassificationMetadata.Element element = new ClassificationMetadata.Element(
					id,
					nullableString( raw, "category" ),
					strings( raw, "spiRoles" ),
					nullableString( raw, "artifact" )
			);
			if ( elements.put( id, element ) != null ) {
				throw new IllegalArgumentException( "Duplicate classification element " + id );
			}
		}
		return new ClassificationMetadata(
				string( root, "hibernateVersion" ),
				string( root, "sourceVersion" ),
				elements
		);
	}

	private static String string(Map<String, Object> values, String name) {
		final String value = nullableString( values, name );
		if ( value == null || value.isBlank() ) {
			throw new IllegalArgumentException( "Missing classification metadata property '" + name + "'" );
		}
		return value;
	}

	private static String nullableString(Map<String, Object> values, String name) {
		final Object value = values.get( name );
		return value == null ? null : value.toString();
	}

	private static Number number(Map<String, Object> values, String name) {
		final Object value = values.get( name );
		if ( value instanceof Number number ) {
			return number;
		}
		throw new IllegalArgumentException( "Missing numeric classification metadata property '" + name + "'" );
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> maps(Map<String, Object> values, String name) {
		final Object value = values.get( name );
		if ( value instanceof List<?> ) {
			return (List<Map<String, Object>>) value;
		}
		return Collections.emptyList();
	}

	private static Set<String> strings(Map<String, Object> values, String name) {
		final Set<String> result = new LinkedHashSet<>();
		final Object value = values.get( name );
		if ( value instanceof List<?> list ) {
			for ( Object item : list ) {
				if ( item != null ) {
					result.add( item.toString() );
				}
			}
		}
		return result;
	}
}
