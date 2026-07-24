/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.Internal;

import static java.util.Objects.requireNonNull;

/// Stable, typed identity for one applied boot mapping.
///
/// Unlike a declaration role, a mapping role identifies a concrete occurrence
/// in an entity, collection, or mapped-superclass application.  Declaration-side
/// compatibility projections are roleless.  The typed root and parts are
/// authoritative; [#getFullPath()] is their canonical diagnostic and archive
/// rendering.
///
/// @since 9.0
/// @author Steve Ebersole
@Internal
public final class MappingRole implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	private final RootKind rootKind;
	private final String rootName;
	private final List<Part> parts;
	private final String fullPath;

	private MappingRole(RootKind rootKind, String rootName, List<Part> parts) {
		this.rootKind = requireNonNull( rootKind );
		this.rootName = requireNonNull( rootName );
		if ( rootName.isBlank() ) {
			throw new IllegalArgumentException( "Mapping role root name cannot be blank" );
		}
		this.parts = List.copyOf( parts );
		this.fullPath = render( rootKind, rootName, parts );
	}

	public static MappingRole entity(String entityName) {
		return root( RootKind.ENTITY, entityName );
	}

	public static MappingRole mappedSuperclass(String className) {
		return root( RootKind.MAPPED_SUPERCLASS, className );
	}

	public static MappingRole collection(String collectionRole) {
		return root( RootKind.COLLECTION, collectionRole );
	}

	public static MappingRole root(RootKind rootKind, String rootName) {
		return new MappingRole( rootKind, rootName, List.of() );
	}

	public MappingRole appendAttribute(String attributeName) {
		return append( PartKind.ATTRIBUTE, attributeName );
	}

	public MappingRole append(PartKind kind) {
		return append( kind, null );
	}

	public MappingRole append(PartKind kind, String name) {
		final ArrayList<Part> result = new ArrayList<>( parts.size() + 1 );
		result.addAll( parts );
		result.add( new Part( kind, name ) );
		return new MappingRole( rootKind, rootName, result );
	}

	public RootKind getRootKind() {
		return rootKind;
	}

	public String getRootName() {
		return rootName;
	}

	public List<Part> getParts() {
		return parts;
	}

	public MappingRole getParent() {
		return parts.isEmpty()
				? null
				: new MappingRole( rootKind, rootName, parts.subList( 0, parts.size() - 1 ) );
	}

	public Part getLocalPart() {
		return parts.isEmpty() ? null : parts.get( parts.size() - 1 );
	}

	public String getFullPath() {
		return fullPath;
	}

	@Override
	public boolean equals(Object object) {
		return this == object
			|| object instanceof MappingRole that
				&& rootKind == that.rootKind
				&& rootName.equals( that.rootName )
				&& parts.equals( that.parts );
	}

	@Override
	public int hashCode() {
		return Objects.hash( rootKind, rootName, parts );
	}

	@Override
	public String toString() {
		return fullPath;
	}

	private static String render(RootKind rootKind, String rootName, List<Part> parts) {
		final StringBuilder result = new StringBuilder()
				.append( rootKind.externalName )
				.append( ':' )
				.append( rootName );
		boolean firstPart = true;
		for ( Part part : parts ) {
			if ( part.kind == PartKind.ATTRIBUTE ) {
				if ( firstPart ) {
					result.append( "#attribute:" );
				}
				else {
					result.append( '.' );
				}
				result.append( part.name );
			}
			else {
				result.append( '#' ).append( part.kind.externalName );
				if ( part.name != null ) {
					result.append( ':' ).append( part.name );
				}
			}
			firstPart = false;
		}
		return result.toString();
	}

	public enum RootKind {
		ENTITY( "entity" ),
		MAPPED_SUPERCLASS( "mapped-superclass" ),
		COLLECTION( "collection" );

		private final String externalName;

		RootKind(String externalName) {
			this.externalName = externalName;
		}
	}

	public enum PartKind {
		ATTRIBUTE( "attribute", true ),
		IDENTIFIER( "identifier", false ),
		IDENTIFIER_MAPPER( "identifier-mapper", false ),
		VERSION( "version", false ),
		DISCRIMINATOR( "discriminator", false ),
		JOIN( "join", true ),
		KEY( "key", false ),
		ELEMENT( "element", false ),
		INDEX( "index", false ),
		COLLECTION_IDENTIFIER( "collection-identifier", false ),
		SOFT_DELETE( "soft-delete", false );

		private final String externalName;
		private final boolean requiresName;

		PartKind(String externalName, boolean requiresName) {
			this.externalName = externalName;
			this.requiresName = requiresName;
		}
	}

	public record Part(PartKind kind, String name) implements Serializable {
		@Serial
		private static final long serialVersionUID = 1L;

		public Part {
			requireNonNull( kind );
			if ( kind.requiresName && ( name == null || name.isBlank() ) ) {
				throw new IllegalArgumentException( "Mapping role part '" + kind + "' requires a name" );
			}
			if ( !kind.requiresName && name != null ) {
				throw new IllegalArgumentException( "Mapping role part '" + kind + "' does not accept a name" );
			}
		}
	}
}
