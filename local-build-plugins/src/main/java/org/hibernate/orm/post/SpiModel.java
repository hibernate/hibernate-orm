/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/// Immutable, analysis-engine-neutral representation of Hibernate's provider
/// SPI surface.
///
/// One [Element] exists for each independently classified or signature-derived
/// declaration. Independently classified declarations retain every
/// classification [Origin] and every useful [ReachabilityPath] by which they
/// were reached from another SPI root.
///
/// @author Steve Ebersole
public final class SpiModel {
	/// The maximum number of shortest deterministic reachability paths retained
	/// for one element. The total number of omitted paths remains available on
	/// [Element#getOmittedReachabilityPathCount()].
	public static final int MAX_REACHABILITY_PATHS = 16;

	private final List<Element> elements;
	private final Map<String, Element> elementsById;

	private SpiModel(Collection<MutableElement> mutableElements) {
		final List<MutableElement> sorted = new ArrayList<>( mutableElements );
		sorted.sort( Comparator.comparing( (element) -> element.id ) );

		final List<Element> elements = new ArrayList<>( sorted.size() );
		final Map<String, Element> elementsById = new LinkedHashMap<>();
		for ( MutableElement mutableElement : sorted ) {
			final Element element = new Element( mutableElement );
			elements.add( element );
			elementsById.put( element.getId(), element );
		}

		this.elements = Collections.unmodifiableList( elements );
		this.elementsById = Collections.unmodifiableMap( elementsById );
	}

	/// Returns all canonical elements in stable identifier order.
	public List<Element> getElements() {
		return elements;
	}

	/// Finds a canonical element by stable identifier.
	public Element getElement(String id) {
		return elementsById.get( id );
	}

	/// Produces a deterministic textual form intended for model and golden-file
	/// tests. It is not the published SPI report format.
	public String snapshot() {
		final StringBuilder snapshot = new StringBuilder();
		for ( Element element : elements ) {
			snapshot.append( element.getId() )
					.append( '|' ).append( element.getKind() )
					.append( '|' ).append( element.getClassification() )
					.append( "|declared=" ).append( element.getDeclaredRoles() )
					.append( "|effective=" ).append( element.getEffectiveRoles() )
					.append( "|origins=" ).append( element.getOrigins() )
					.append( "|api=" ).append( element.getApplicationApiStatus() )
					.append( "|lifecycle=" ).append( element.getLifecycle() )
					.append( "|source=" ).append( element.getSource() )
					.append( "|exceptions=" ).append( element.getMigrationExceptions() )
					.append( "|paths=" ).append( element.getReachabilityPaths() )
					.append( "|omittedPaths=" ).append( element.getOmittedReachabilityPathCount() )
					.append( '\n' );
		}
		return snapshot.toString();
	}

	/// Creates the mutable collector used by analysis-engine adapters.
	static Builder builder() {
		return new Builder();
	}

	/// The independent provider roles.
	public enum Role {
		USE,
		IMPLEMENT,
		SUPPLY
	}

	/// The kind of declaration represented by an element.
	public enum ElementKind {
		PACKAGE,
		TYPE,
		ANNOTATION_TYPE,
		CONSTRUCTOR,
		METHOD,
		FIELD
	}

	/// Whether an element has its own SPI classification or is only required by
	/// a supported signature.
	public enum Classification {
		INDEPENDENT,
		SIGNATURE_DERIVED
	}

	/// How an independent classification was conferred.
	public enum OriginKind {
		DIRECT,
		PACKAGE,
		ENCLOSING_TYPE,
		EXACT_SPI_PACKAGE
	}

	/// Application API status, kept independently from provider SPI status.
	public enum ApiStatus {
		API,
		NON_API,
		UNKNOWN
	}

	/// One origin contributing roles to an independently classified element.
	public static final class Origin implements Comparable<Origin> {
		private final OriginKind kind;
		private final String sourceElementId;
		private final Set<Role> roles;

		public Origin(OriginKind kind, String sourceElementId, Collection<Role> roles) {
			this.kind = kind;
			this.sourceElementId = sourceElementId;
			this.roles = immutableRoles( roles );
		}

		public OriginKind getKind() {
			return kind;
		}

		public String getSourceElementId() {
			return sourceElementId;
		}

		public Set<Role> getRoles() {
			return roles;
		}

		@Override
		public int compareTo(Origin other) {
			int comparison = kind.compareTo( other.kind );
			if ( comparison == 0 ) {
				comparison = sourceElementId.compareTo( other.sourceElementId );
			}
			if ( comparison == 0 ) {
				comparison = roles.toString().compareTo( other.roles.toString() );
			}
			return comparison;
		}

		@Override
		public boolean equals(Object object) {
			if ( this == object ) {
				return true;
			}
			if ( !(object instanceof Origin) ) {
				return false;
			}
			final Origin other = (Origin) object;
			return kind == other.kind
					&& sourceElementId.equals( other.sourceElementId )
					&& roles.equals( other.roles );
		}

		@Override
		public int hashCode() {
			int result = kind.hashCode();
			result = 31 * result + sourceElementId.hashCode();
			result = 31 * result + roles.hashCode();
			return result;
		}

		@Override
		public String toString() {
			return kind + "(" + sourceElementId + ":" + roles + ")";
		}
	}

	/// Orthogonal lifecycle and internal-status metadata.
	public static final class Lifecycle {
		private final boolean internal;
		private final boolean incubating;
		private final boolean deprecated;

		public Lifecycle(boolean internal, boolean incubating, boolean deprecated) {
			this.internal = internal;
			this.incubating = incubating;
			this.deprecated = deprecated;
		}

		public boolean isInternal() {
			return internal;
		}

		public boolean isIncubating() {
			return incubating;
		}

		public boolean isDeprecated() {
			return deprecated;
		}

		private Lifecycle merge(Lifecycle other) {
			return new Lifecycle(
					internal || other.internal,
					incubating || other.incubating,
					deprecated || other.deprecated
			);
		}

		@Override
		public String toString() {
			return "internal=" + internal + ",incubating=" + incubating + ",deprecated=" + deprecated;
		}
	}

	/// A shortest useful path from an independently classified root to an
	/// element required by its supported surface.
	public static final class ReachabilityPath implements Comparable<ReachabilityPath> {
		private final List<String> elementIds;

		public ReachabilityPath(Collection<String> elementIds) {
			this.elementIds = Collections.unmodifiableList( new ArrayList<>( elementIds ) );
		}

		public List<String> getElementIds() {
			return elementIds;
		}

		@Override
		public int compareTo(ReachabilityPath other) {
			int comparison = Integer.compare( elementIds.size(), other.elementIds.size() );
			if ( comparison != 0 ) {
				return comparison;
			}
			for ( int i = 0; i < elementIds.size(); i++ ) {
				comparison = elementIds.get( i ).compareTo( other.elementIds.get( i ) );
				if ( comparison != 0 ) {
					return comparison;
				}
			}
			return 0;
		}

		@Override
		public boolean equals(Object object) {
			return object instanceof ReachabilityPath
					&& elementIds.equals( ((ReachabilityPath) object).elementIds );
		}

		@Override
		public int hashCode() {
			return elementIds.hashCode();
		}

		@Override
		public String toString() {
			return String.join( " -> ", elementIds );
		}
	}

	/// One canonical SPI element.
	public static final class Element {
		private final String id;
		private final ElementKind kind;
		private final String declaringPackage;
		private final String signature;
		private final Classification classification;
		private final Set<Role> declaredRoles;
		private final Set<Role> effectiveRoles;
		private final SortedSet<Origin> origins;
		private final ApiStatus applicationApiStatus;
		private final Lifecycle lifecycle;
		private final String source;
		private final SortedSet<String> migrationExceptions;
		private final List<ReachabilityPath> reachabilityPaths;
		private final int omittedReachabilityPathCount;

		private Element(MutableElement mutable) {
			id = mutable.id;
			kind = mutable.kind;
			declaringPackage = mutable.declaringPackage;
			signature = mutable.signature;
			classification = mutable.classification;
			declaredRoles = immutableRoles( mutable.declaredRoles );
			effectiveRoles = immutableRoles( mutable.effectiveRoles );
			origins = Collections.unmodifiableSortedSet( new TreeSet<>( mutable.origins ) );
			applicationApiStatus = mutable.applicationApiStatus;
			lifecycle = mutable.lifecycle;
			source = mutable.source;
			migrationExceptions = Collections.unmodifiableSortedSet( new TreeSet<>( mutable.migrationExceptions ) );
			reachabilityPaths = Collections.unmodifiableList( new ArrayList<>( mutable.reachabilityPaths ) );
			omittedReachabilityPathCount = Math.max( 0, mutable.observedReachabilityPathCount - reachabilityPaths.size() );
		}

		public String getId() {
			return id;
		}

		public ElementKind getKind() {
			return kind;
		}

		public String getDeclaringPackage() {
			return declaringPackage;
		}

		public String getSignature() {
			return signature;
		}

		public Classification getClassification() {
			return classification;
		}

		public Set<Role> getDeclaredRoles() {
			return declaredRoles;
		}

		public Set<Role> getEffectiveRoles() {
			return effectiveRoles;
		}

		public SortedSet<Origin> getOrigins() {
			return origins;
		}

		public ApiStatus getApplicationApiStatus() {
			return applicationApiStatus;
		}

		public Lifecycle getLifecycle() {
			return lifecycle;
		}

		public String getSource() {
			return source;
		}

		public SortedSet<String> getMigrationExceptions() {
			return migrationExceptions;
		}

		public List<ReachabilityPath> getReachabilityPaths() {
			return reachabilityPaths;
		}

		public int getOmittedReachabilityPathCount() {
			return omittedReachabilityPathCount;
		}
	}

	static final class Builder {
		private final Map<String, MutableElement> elements = new LinkedHashMap<>();

		void classify(
				String id,
				ElementKind kind,
				String declaringPackage,
				String signature,
				Collection<Role> directlyDeclaredRoles,
				Origin origin,
				ApiStatus applicationApiStatus,
				Lifecycle lifecycle,
				String source,
				Collection<String> migrationExceptions) {
			final MutableElement element = element(
					id,
					kind,
					declaringPackage,
					signature,
					applicationApiStatus,
					lifecycle,
					source,
					migrationExceptions
			);
			element.classification = Classification.INDEPENDENT;
			element.declaredRoles.addAll( directlyDeclaredRoles );
			element.effectiveRoles.addAll( origin.getRoles() );
			element.origins.add( origin );
		}

		void derived(
				String id,
				ElementKind kind,
				String declaringPackage,
				String signature,
				ApiStatus applicationApiStatus,
				Lifecycle lifecycle,
				String source,
				Collection<String> migrationExceptions) {
			element(
					id,
					kind,
					declaringPackage,
					signature,
					applicationApiStatus,
					lifecycle,
					source,
					migrationExceptions
			);
		}

		private MutableElement element(
				String id,
				ElementKind kind,
				String declaringPackage,
				String signature,
				ApiStatus applicationApiStatus,
				Lifecycle lifecycle,
				String source,
				Collection<String> migrationExceptions) {
			MutableElement element = elements.get( id );
			if ( element == null ) {
				element = new MutableElement(
						id,
						kind,
						declaringPackage,
						signature,
						applicationApiStatus,
						lifecycle,
						source,
						migrationExceptions
				);
				elements.put( id, element );
			}
			else {
				element.mergeMetadata( applicationApiStatus, lifecycle, source, migrationExceptions );
			}
			return element;
		}

		void addReachabilityPath(String elementId, ReachabilityPath path) {
			final MutableElement element = elements.get( elementId );
			if ( element == null ) {
				throw new IllegalArgumentException( "Unknown SPI element: " + elementId );
			}
			if ( element.reachabilityPaths.contains( path ) ) {
				return;
			}
			element.observedReachabilityPathCount++;
			element.reachabilityPaths.add( path );
			if ( element.reachabilityPaths.size() > MAX_REACHABILITY_PATHS ) {
				element.reachabilityPaths.remove( element.reachabilityPaths.last() );
			}
		}

		boolean isIndependent(String elementId) {
			final MutableElement element = elements.get( elementId );
			return element != null && element.classification == Classification.INDEPENDENT;
		}

		Set<Role> effectiveRoles(String elementId) {
			final MutableElement element = elements.get( elementId );
			if ( element == null ) {
				return Collections.emptySet();
			}
			return immutableRoles( element.effectiveRoles );
		}

		SortedSet<String> independentElementIds() {
			final SortedSet<String> ids = new TreeSet<>();
			for ( MutableElement element : elements.values() ) {
				if ( element.classification == Classification.INDEPENDENT ) {
					ids.add( element.id );
				}
			}
			return ids;
		}

		SortedSet<String> elementsWithOrigin(OriginKind kind, String sourceElementId) {
			final SortedSet<String> ids = new TreeSet<>();
			for ( MutableElement element : elements.values() ) {
				for ( Origin origin : element.origins ) {
					if ( origin.getKind() == kind && origin.getSourceElementId().equals( sourceElementId ) ) {
						ids.add( element.id );
					}
				}
			}
			return ids;
		}

		SpiModel build() {
			return new SpiModel( elements.values() );
		}
	}

	private static final class MutableElement {
		private final String id;
		private final ElementKind kind;
		private final String declaringPackage;
		private final String signature;
		private Classification classification = Classification.SIGNATURE_DERIVED;
		private final EnumSet<Role> declaredRoles = EnumSet.noneOf( Role.class );
		private final EnumSet<Role> effectiveRoles = EnumSet.noneOf( Role.class );
		private final SortedSet<Origin> origins = new TreeSet<>();
		private ApiStatus applicationApiStatus;
		private Lifecycle lifecycle;
		private String source;
		private final SortedSet<String> migrationExceptions = new TreeSet<>();
		private final SortedSet<ReachabilityPath> reachabilityPaths = new TreeSet<>();
		private int observedReachabilityPathCount;

		private MutableElement(
				String id,
				ElementKind kind,
				String declaringPackage,
				String signature,
				ApiStatus applicationApiStatus,
				Lifecycle lifecycle,
				String source,
				Collection<String> migrationExceptions) {
			this.id = id;
			this.kind = kind;
			this.declaringPackage = declaringPackage;
			this.signature = signature;
			this.applicationApiStatus = applicationApiStatus;
			this.lifecycle = lifecycle;
			this.source = source;
			this.migrationExceptions.addAll( migrationExceptions );
		}

		private void mergeMetadata(
				ApiStatus applicationApiStatus,
				Lifecycle lifecycle,
				String source,
				Collection<String> migrationExceptions) {
			if ( this.applicationApiStatus == ApiStatus.UNKNOWN ) {
				this.applicationApiStatus = applicationApiStatus;
			}
			else if ( applicationApiStatus != ApiStatus.UNKNOWN
					&& this.applicationApiStatus != applicationApiStatus ) {
				throw new IllegalArgumentException( "Conflicting application API status for " + id );
			}
			this.lifecycle = this.lifecycle.merge( lifecycle );
			if ( "unknown".equals( this.source ) ) {
				this.source = source;
			}
			else if ( !"unknown".equals( source ) && !this.source.equals( source ) ) {
				throw new IllegalArgumentException( "Conflicting source for " + id );
			}
			this.migrationExceptions.addAll( migrationExceptions );
		}
	}

	private static Set<Role> immutableRoles(Collection<Role> roles) {
		final EnumSet<Role> copy = roles.isEmpty() ? EnumSet.noneOf( Role.class ) : EnumSet.copyOf( roles );
		return Collections.unmodifiableSet( copy );
	}
}
