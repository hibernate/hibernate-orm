/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.spi;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/// Stable, typed identity for one applied boot mapping.
///
/// Unlike a declaration role, a mapping role identifies a concrete occurrence
/// in an entity, collection, or mapped-superclass application.  Declaration-side
/// compatibility projections are roleless.  The typed root and parts are
/// authoritative; [#getFullPath()] is their canonical diagnostic and archive
/// rendering.
///
/// Roles are immutable. Appending a part returns a new role and leaves the
/// original unchanged. Equality is based on the structured root and parts, not
/// on the rendered path.
///
/// @since 9.0
/// @author Steve Ebersole
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

	/// Creates the root role for an entity mapping.
	public static MappingRole entity(String entityName) {
		return root( RootKind.ENTITY, entityName );
	}

	/// Creates the root role for a mapped-superclass application.
	public static MappingRole mappedSuperclass(String className) {
		return root( RootKind.MAPPED_SUPERCLASS, className );
	}

	/// Creates the root role for a persistent collection.
	public static MappingRole collection(String collectionRole) {
		return root( RootKind.COLLECTION, collectionRole );
	}

	/// Creates a root role of the given kind.
	public static MappingRole root(RootKind rootKind, String rootName) {
		return new MappingRole( rootKind, rootName, List.of() );
	}

	/// Appends a named attribute part.
	public MappingRole appendAttribute(String attributeName) {
		return append( PartKind.ATTRIBUTE, attributeName );
	}

	/// Appends an unnamed part.
	///
	/// @throws IllegalArgumentException if the part kind requires a name
	public MappingRole append(PartKind kind) {
		return append( kind, null );
	}

	/// Appends a part with its required name, if any.
	///
	/// @throws IllegalArgumentException if the presence of `name` does not
	/// match the requirements of `kind`
	public MappingRole append(PartKind kind, String name) {
		final ArrayList<Part> result = new ArrayList<>( parts.size() + 1 );
		result.addAll( parts );
		result.add( new Part( kind, name ) );
		return new MappingRole( rootKind, rootName, result );
	}

	/// The kind of mapping root.
	public RootKind getRootKind() {
		return rootKind;
	}

	/// The entity name, mapped-superclass class name, or collection role which
	/// identifies the root.
	public String getRootName() {
		return rootName;
	}

	/// The immutable ordered path parts below the root.
	public List<Part> getParts() {
		return parts;
	}

	/// The enclosing role, or `null` when this is a root role.
	public MappingRole getParent() {
		return parts.isEmpty()
				? null
				: new MappingRole( rootKind, rootName, parts.subList( 0, parts.size() - 1 ) );
	}

	/// The final path part, or `null` when this is a root role.
	public Part getLocalPart() {
		return parts.isEmpty() ? null : parts.get( parts.size() - 1 );
	}

	/// The canonical diagnostic and archive rendering.
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

	/// The kinds of root mapping identified by a role.
	public enum RootKind {
		/// An entity mapping root, named by Hibernate entity name.
		ENTITY( "entity" ),
		/// A mapped-superclass application root, named by Java class name.
		MAPPED_SUPERCLASS( "mapped-superclass" ),
		/// A persistent collection root, named by collection role.
		COLLECTION( "collection" );

		private final String externalName;

		RootKind(String externalName) {
			this.externalName = externalName;
		}
	}

	/// The kinds of part which may occur below a mapping root.
	public enum PartKind {
		/// A named persistent attribute.
		ATTRIBUTE( "attribute", true ),
		/// The entity identifier.
		IDENTIFIER( "identifier", false ),
		/// The compatibility identifier-mapper component.
		IDENTIFIER_MAPPER( "identifier-mapper", false ),
		/// The entity version value.
		VERSION( "version", false ),
		/// An entity or polymorphic-embeddable discriminator.
		DISCRIMINATOR( "discriminator", false ),
		/// A named secondary-table join.
		JOIN( "join", true ),
		/// A collection or association key.
		KEY( "key", false ),
		/// A collection element.
		ELEMENT( "element", false ),
		/// A collection index or map key.
		INDEX( "index", false ),
		/// An id-bag collection identifier.
		COLLECTION_IDENTIFIER( "collection-identifier", false ),
		/// A soft-delete mapping value.
		SOFT_DELETE( "soft-delete", false );

		private final String externalName;
		private final boolean requiresName;

		PartKind(String externalName, boolean requiresName) {
			this.externalName = externalName;
			this.requiresName = requiresName;
		}
	}

	/// One structured path segment of a mapping role.
	public record Part(
			/// The semantic kind of the segment.
			PartKind kind,
			/// The segment name when required by its kind; otherwise `null`.
			String name)
			implements Serializable {
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
