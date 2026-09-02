/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.lang.reflect.Modifier;
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

/// Immutable, analysis-engine-neutral representation of Hibernate's API, SPI,
/// and internal declaration classifications.
///
/// Each Hibernate-owned declaration has one canonical [Element]. Valid
/// elements resolve to exactly one [Category]. More-specific classification
/// evidence takes precedence over less-specific evidence, while equally
/// specific conflicting evidence is retained for validation.
/// Signature relationships are stored as direct typed [Reference] instances;
/// root-to-element paths are intentionally not materialized.
///
/// @author Steve Ebersole
public final class ClassificationModel {
	private final List<Element> elements;
	private final Map<String, Element> elementsById;

	private ClassificationModel(Collection<MutableElement> mutableElements) {
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

	/// Produces a deterministic form for focused model tests. This is not the
	/// published classification JSON format.
	public String snapshot() {
		final StringBuilder snapshot = new StringBuilder();
		for ( Element element : elements ) {
			snapshot.append( element.getId() )
					.append( '|' ).append( element.getKind() )
					.append( "|owner=" ).append( element.getOwnerId() )
					.append( "|classificationStatus=" ).append( element.getClassificationStatus() )
					.append( "|category=" ).append( element.getCategory() )
					.append( "|categoryEvidence=" ).append( element.getCategoryEvidence() )
					.append( "|declaredRoles=" ).append( element.getDeclaredRoles() )
					.append( "|effectiveRoles=" ).append( element.getEffectiveRoles() )
					.append( "|origins=" ).append( element.getClassificationOrigins() )
					.append( "|structure=" ).append( element.getStructure() )
					.append( "|lifecycle=" ).append( element.getLifecycle() )
					.append( "|artifact=" ).append( element.getArtifact() )
					.append( "|references=" ).append( element.getReferences() )
					.append( '\n' );
		}
		return snapshot.toString();
	}

	static Builder builder() {
		return new Builder();
	}

	/// The mutually exclusive supported-audience categories.
	public enum Category {
		API,
		SPI,
		INTERNAL
	}

	/// Whether category evidence resolves to a valid effective category.
	public enum ClassificationStatus {
		RESOLVED,
		CONFLICTING,
		UNCLASSIFIED
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

	/// How category or SPI-role intent was conferred.
	public enum OriginKind {
		DIRECT( 4 ),
		PACKAGE( 2 ),
		ENCLOSING_TYPE( 3 ),
		SPI_PACKAGE( 1 ),
		INTERNAL_PACKAGE( 1 ),
		ORDINARY_API( 0 );

		private final int precedence;

		OriginKind(int precedence) {
			this.precedence = precedence;
		}

		/// Relative specificity used to resolve mutually exclusive categories.
		public int getPrecedence() {
			return precedence;
		}
	}

	/// Orthogonal lifecycle states.
	public enum LifecycleState {
		INCUBATING,
		DEPRECATED,
		FOR_REMOVAL,
		REMOVAL
	}

	/// How lifecycle metadata was conferred.
	public enum LifecycleOriginKind {
		DIRECT,
		PACKAGE,
		ENCLOSING_TYPE
	}

	/// The supported-signature position represented by a direct edge.
	public enum ReferenceKind {
		SUPERCLASS,
		IMPLEMENTED_INTERFACE,
		METHOD_RETURN,
		METHOD_PARAMETER,
		CONSTRUCTOR_PARAMETER,
		FIELD_TYPE,
		GENERIC_ARGUMENT,
		GENERIC_BOUND,
		ARRAY_COMPONENT,
		DECLARED_CHECKED_EXCEPTION,
		ANNOTATION_MEMBER_TYPE,
		ANNOTATION_CLASS_SELECTION,
		EXPOSED_NESTED_TYPE
	}

	/// Whether a referenced declaration belongs to the indexed Hibernate model.
	public enum ReferenceTarget {
		HIBERNATE,
		EXTERNAL
	}

	/// Compiled declaration structure needed by validation and compatibility
	/// without exposing Jandex types outside the ingestion adapter.
	public static final class Structure {
		public static final Structure UNKNOWN = new Structure( 0, false, false, false );

		private final int modifiers;
		private final boolean interfaceType;
		private final boolean declaringTypeFinal;
		private final boolean known;

		public Structure(int modifiers, boolean interfaceType, boolean declaringTypeFinal) {
			this( modifiers, interfaceType, declaringTypeFinal, true );
		}

		private Structure(int modifiers, boolean interfaceType, boolean declaringTypeFinal, boolean known) {
			this.modifiers = modifiers;
			this.interfaceType = interfaceType;
			this.declaringTypeFinal = declaringTypeFinal;
			this.known = known;
		}

		public int getModifiers() {
			return modifiers;
		}

		public boolean isKnown() {
			return known;
		}

		public boolean isInterfaceType() {
			return interfaceType;
		}

		public boolean isDeclaringTypeFinal() {
			return declaringTypeFinal;
		}

		public boolean isPublic() {
			return Modifier.isPublic( modifiers );
		}

		public boolean isProtected() {
			return Modifier.isProtected( modifiers );
		}

		public boolean isPrivate() {
			return Modifier.isPrivate( modifiers );
		}

		public boolean isStatic() {
			return Modifier.isStatic( modifiers );
		}

		public boolean isFinal() {
			return Modifier.isFinal( modifiers );
		}

		public boolean isAbstract() {
			return Modifier.isAbstract( modifiers );
		}

		public boolean isExternallyAccessible() {
			return isPublic() || isProtected();
		}

		public boolean isOverridableMethod() {
			return !isPrivate() && !isStatic() && !isFinal() && !declaringTypeFinal;
		}

		@Override
		public String toString() {
			return known
					? "modifiers=" + modifiers + ",interface=" + interfaceType + ",declaringTypeFinal=" + declaringTypeFinal
					: "unknown";
		}
	}

	/// Evidence contributing an API, SPI, or internal category. SPI evidence may
	/// additionally contribute provider roles.
	public static final class ClassificationOrigin implements Comparable<ClassificationOrigin> {
		private final Category category;
		private final OriginKind kind;
		private final String sourceElementId;
		private final Set<Role> roles;

		public ClassificationOrigin(
				Category category,
				OriginKind kind,
				String sourceElementId,
				Collection<Role> roles) {
			this.category = category;
			this.kind = kind;
			this.sourceElementId = sourceElementId;
			this.roles = immutableRoles( roles );
			if ( category != Category.SPI && !this.roles.isEmpty() ) {
				throw new IllegalArgumentException( "Only SPI origins may define roles" );
			}
		}

		public Category getCategory() {
			return category;
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
		public int compareTo(ClassificationOrigin other) {
			int comparison = category.compareTo( other.category );
			if ( comparison == 0 ) {
				comparison = kind.compareTo( other.kind );
			}
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
			if ( !(object instanceof ClassificationOrigin) ) {
				return false;
			}
			final ClassificationOrigin other = (ClassificationOrigin) object;
			return category == other.category
					&& kind == other.kind
					&& sourceElementId.equals( other.sourceElementId )
					&& roles.equals( other.roles );
		}

		@Override
		public int hashCode() {
			int result = category.hashCode();
			result = 31 * result + kind.hashCode();
			result = 31 * result + sourceElementId.hashCode();
			result = 31 * result + roles.hashCode();
			return result;
		}

		@Override
		public String toString() {
			return category + ":" + kind + "(" + sourceElementId + ":" + roles + ")";
		}
	}

	/// One origin of an orthogonal lifecycle state.
	public static final class LifecycleOrigin implements Comparable<LifecycleOrigin> {
		private final LifecycleState state;
		private final LifecycleOriginKind kind;
		private final String sourceElementId;

		public LifecycleOrigin(LifecycleState state, LifecycleOriginKind kind, String sourceElementId) {
			this.state = state;
			this.kind = kind;
			this.sourceElementId = sourceElementId;
		}

		public LifecycleState getState() {
			return state;
		}

		public LifecycleOriginKind getKind() {
			return kind;
		}

		public String getSourceElementId() {
			return sourceElementId;
		}

		@Override
		public int compareTo(LifecycleOrigin other) {
			int comparison = state.compareTo( other.state );
			if ( comparison == 0 ) {
				comparison = kind.compareTo( other.kind );
			}
			return comparison == 0 ? sourceElementId.compareTo( other.sourceElementId ) : comparison;
		}

		@Override
		public boolean equals(Object object) {
			return object instanceof LifecycleOrigin
					&& compareTo( (LifecycleOrigin) object ) == 0;
		}

		@Override
		public int hashCode() {
			int result = state.hashCode();
			result = 31 * result + kind.hashCode();
			result = 31 * result + sourceElementId.hashCode();
			return result;
		}

		@Override
		public String toString() {
			return state + ":" + kind + "(" + sourceElementId + ")";
		}
	}

	/// Orthogonal lifecycle metadata and its complete origin set.
	public static final class Lifecycle {
		private final SortedSet<LifecycleOrigin> origins;

		private Lifecycle(Collection<LifecycleOrigin> origins) {
			this.origins = Collections.unmodifiableSortedSet( new TreeSet<>( origins ) );
		}

		public SortedSet<LifecycleOrigin> getOrigins() {
			return origins;
		}

		public boolean isIncubating() {
			return has( LifecycleState.INCUBATING );
		}

		public boolean isDeprecated() {
			return has( LifecycleState.DEPRECATED );
		}

		public boolean isForRemoval() {
			return has( LifecycleState.FOR_REMOVAL );
		}

		public boolean isRemoval() {
			return has( LifecycleState.REMOVAL );
		}

		private boolean has(LifecycleState state) {
			for ( LifecycleOrigin origin : origins ) {
				if ( origin.getState() == state ) {
					return true;
				}
			}
			return false;
		}

		@Override
		public String toString() {
			return origins.toString();
		}
	}

	/// One direct typed supported-signature edge.
	public static final class Reference implements Comparable<Reference> {
		private final ReferenceKind kind;
		private final String targetElementId;
		private final ReferenceTarget target;

		public Reference(ReferenceKind kind, String targetElementId, ReferenceTarget target) {
			this.kind = kind;
			this.targetElementId = targetElementId;
			this.target = target;
		}

		public ReferenceKind getKind() {
			return kind;
		}

		public String getTargetElementId() {
			return targetElementId;
		}

		public ReferenceTarget getTarget() {
			return target;
		}

		@Override
		public int compareTo(Reference other) {
			int comparison = kind.compareTo( other.kind );
			if ( comparison == 0 ) {
				comparison = targetElementId.compareTo( other.targetElementId );
			}
			return comparison == 0 ? target.compareTo( other.target ) : comparison;
		}

		@Override
		public boolean equals(Object object) {
			return object instanceof Reference && compareTo( (Reference) object ) == 0;
		}

		@Override
		public int hashCode() {
			int result = kind.hashCode();
			result = 31 * result + targetElementId.hashCode();
			result = 31 * result + target.hashCode();
			return result;
		}

		@Override
		public String toString() {
			return kind + "->" + targetElementId + "[" + target + "]";
		}
	}

	/// One canonical Hibernate-owned declaration.
	public static final class Element {
		private final String id;
		private final ElementKind kind;
		private final String ownerId;
		private final Structure structure;
		private final ClassificationStatus classificationStatus;
		private final Category category;
		private final Set<Category> categoryEvidence;
		private final Set<Role> declaredRoles;
		private final Set<Role> effectiveRoles;
		private final SortedSet<ClassificationOrigin> classificationOrigins;
		private final Lifecycle lifecycle;
		private final String artifact;
		private final SortedSet<Reference> references;

		private Element(MutableElement mutable) {
			id = mutable.id;
			kind = mutable.kind;
			ownerId = mutable.ownerId;
			structure = mutable.structure;
			classificationOrigins = Collections.unmodifiableSortedSet( new TreeSet<>( mutable.classificationOrigins ) );
			final Set<Category> evidence = controllingCategoryEvidence( classificationOrigins );
			categoryEvidence = Collections.unmodifiableSet( evidence );
			if ( evidence.size() == 1 ) {
				classificationStatus = ClassificationStatus.RESOLVED;
				category = evidence.iterator().next();
			}
			else if ( evidence.isEmpty() ) {
				classificationStatus = ClassificationStatus.UNCLASSIFIED;
				category = null;
			}
			else {
				classificationStatus = ClassificationStatus.CONFLICTING;
				category = null;
			}
			declaredRoles = immutableRoles( mutable.declaredRoles );
			this.effectiveRoles = immutableRoles( effectiveRoles( classificationOrigins, category ) );
			lifecycle = new Lifecycle( mutable.lifecycleOrigins );
			artifact = mutable.artifact;
			references = Collections.unmodifiableSortedSet( new TreeSet<>( mutable.references ) );
		}

		public String getId() {
			return id;
		}

		public ElementKind getKind() {
			return kind;
		}

		public String getOwnerId() {
			return ownerId;
		}

		public Structure getStructure() {
			return structure;
		}

		public ClassificationStatus getClassificationStatus() {
			return classificationStatus;
		}

		/// Returns the one effective category, or `null` when evidence conflicts
		/// or no category has been assigned.
		public Category getCategory() {
			return category;
		}

		public Set<Category> getCategoryEvidence() {
			return categoryEvidence;
		}

		public boolean hasCategoryConflict() {
			return categoryEvidence.size() > 1;
		}

		public Set<Role> getDeclaredRoles() {
			return declaredRoles;
		}

		public Set<Role> getEffectiveRoles() {
			return effectiveRoles;
		}

		public SortedSet<ClassificationOrigin> getClassificationOrigins() {
			return classificationOrigins;
		}

		public Lifecycle getLifecycle() {
			return lifecycle;
		}

		public String getArtifact() {
			return artifact;
		}

		public SortedSet<Reference> getReferences() {
			return references;
		}
	}

	static final class Builder {
		private final Map<String, MutableElement> elements = new LinkedHashMap<>();

		void declaration(
				String id,
				ElementKind kind,
				String ownerId,
				Structure structure,
				String artifact) {
			final MutableElement existing = elements.get( id );
			if ( existing == null ) {
				elements.put(
						id,
						new MutableElement( id, kind, ownerId, structure, artifact )
				);
				return;
			}
			existing.verifyDeclaration( kind, ownerId, structure, artifact );
		}

		boolean contains(String id) {
			return elements.containsKey( id );
		}

		void addClassificationOrigin(
				String elementId,
				ClassificationOrigin origin,
				Collection<Role> directlyDeclaredRoles) {
			final MutableElement element = required( elementId );
			element.classificationOrigins.add( origin );
			element.declaredRoles.addAll( directlyDeclaredRoles );
		}

		boolean hasClassificationEvidence(String elementId) {
			return !required( elementId ).classificationOrigins.isEmpty();
		}

		Set<Role> effectiveRoles(String elementId) {
			final SortedSet<ClassificationOrigin> origins = required( elementId ).classificationOrigins;
			final Set<Category> evidence = controllingCategoryEvidence( origins );
			final Category category = evidence.size() == 1 ? evidence.iterator().next() : null;
			return Collections.unmodifiableSet( ClassificationModel.effectiveRoles( origins, category ) );
		}

		Set<Category> categoryEvidence(String elementId) {
			return Collections.unmodifiableSet(
					controllingCategoryEvidence( required( elementId ).classificationOrigins )
			);
		}

		void addLifecycleOrigin(String elementId, LifecycleOrigin origin) {
			required( elementId ).lifecycleOrigins.add( origin );
		}

		void addReference(String elementId, Reference reference) {
			required( elementId ).references.add( reference );
		}

		void retainReportableSurface() {
			final Set<String> requiredIds = new TreeSet<>();
			final java.util.ArrayDeque<String> pending = new java.util.ArrayDeque<>();
			for ( MutableElement element : elements.values() ) {
				if ( element.kind == ElementKind.PACKAGE || !element.classificationOrigins.isEmpty() ) {
					requiredIds.add( element.id );
					pending.add( element.id );
				}
			}

			while ( !pending.isEmpty() ) {
				final MutableElement element = elements.get( pending.removeFirst() );
				if ( element == null ) {
					continue;
				}
				if ( element.ownerId != null && requiredIds.add( element.ownerId ) ) {
					pending.addLast( element.ownerId );
				}
				for ( Reference reference : element.references ) {
					if ( reference.getTarget() == ReferenceTarget.HIBERNATE
							&& requiredIds.add( reference.getTargetElementId() ) ) {
						pending.addLast( reference.getTargetElementId() );
					}
				}
			}

			elements.entrySet().removeIf(
					(entry) -> !requiredIds.contains( entry.getKey() )
			);
		}

		ClassificationModel build() {
			return new ClassificationModel( elements.values() );
		}

		private MutableElement required(String elementId) {
			final MutableElement element = elements.get( elementId );
			if ( element == null ) {
				throw new IllegalArgumentException( "Unknown classification element: " + elementId );
			}
			return element;
		}
	}

	private static final class MutableElement {
		private final String id;
		private final ElementKind kind;
		private final String ownerId;
		private final Structure structure;
		private final String artifact;
		private final EnumSet<Role> declaredRoles = EnumSet.noneOf( Role.class );
		private final SortedSet<ClassificationOrigin> classificationOrigins = new TreeSet<>();
		private final SortedSet<LifecycleOrigin> lifecycleOrigins = new TreeSet<>();
		private final SortedSet<Reference> references = new TreeSet<>();

		private MutableElement(
				String id,
				ElementKind kind,
				String ownerId,
				Structure structure,
				String artifact) {
			this.id = id;
			this.kind = kind;
			this.ownerId = ownerId;
			this.structure = structure;
			this.artifact = artifact;
		}

		private void verifyDeclaration(
				ElementKind kind,
				String ownerId,
				Structure structure,
				String artifact) {
			if ( this.kind != kind
					|| !equal( this.ownerId, ownerId )
					|| !equalStructure( this.structure, structure )
					|| !this.artifact.equals( artifact ) ) {
				throw new IllegalArgumentException( "Conflicting declaration metadata for " + id );
			}
		}

		private static boolean equal(Object first, Object second) {
			return first == null ? second == null : first.equals( second );
		}

		private static boolean equalStructure(Structure first, Structure second) {
			return first.isKnown() == second.isKnown()
					&& first.getModifiers() == second.getModifiers()
					&& first.isInterfaceType() == second.isInterfaceType()
					&& first.isDeclaringTypeFinal() == second.isDeclaringTypeFinal();
		}
	}

	private static Set<Role> immutableRoles(Collection<Role> roles) {
		final EnumSet<Role> copy = roles.isEmpty() ? EnumSet.noneOf( Role.class ) : EnumSet.copyOf( roles );
		return Collections.unmodifiableSet( copy );
	}

	private static EnumSet<Category> controllingCategoryEvidence(Collection<ClassificationOrigin> origins) {
		final EnumSet<Category> categories = EnumSet.noneOf( Category.class );
		int precedence = Integer.MIN_VALUE;
		for ( ClassificationOrigin origin : origins ) {
			final int originPrecedence = origin.getKind().getPrecedence();
			if ( originPrecedence > precedence ) {
				categories.clear();
				precedence = originPrecedence;
			}
			if ( originPrecedence == precedence ) {
				categories.add( origin.getCategory() );
			}
		}
		return categories;
	}

	private static EnumSet<Role> effectiveRoles(
			Collection<ClassificationOrigin> origins,
			Category effectiveCategory) {
		final EnumSet<Role> roles = EnumSet.noneOf( Role.class );
		if ( effectiveCategory != Category.SPI ) {
			return roles;
		}
		for ( ClassificationOrigin origin : origins ) {
			if ( origin.getCategory() == Category.SPI ) {
				roles.addAll( origin.getRoles() );
			}
		}
		return roles;
	}
}
