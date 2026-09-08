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
import java.util.List;
import java.util.Set;

import static org.hibernate.orm.post.ClassificationModel.Category.API;
import static org.hibernate.orm.post.ClassificationModel.Category.SPI;
import static org.hibernate.orm.post.ClassificationModel.ClassificationStatus.RESOLVED;
import static org.hibernate.orm.post.ClassificationModel.ElementKind.PACKAGE;
import static org.hibernate.orm.post.ClassificationModel.Role.IMPLEMENT;
import static org.hibernate.orm.post.ClassificationModel.Role.SUPPLY;
import static org.hibernate.orm.post.ClassificationModel.Role.USE;
import static org.hibernate.orm.post.JavaMigrationCompatibilityAnalyzer.Cause;

/// Applies Hibernate's classification, lifecycle, release-horizon, and SPI-role
/// policies to Java migration changes.
///
/// API compatibility is enforced within one major release. Stable SPI
/// compatibility is enforced within one `X.Y` release family. Incubating
/// declarations are intentionally excluded from both guarantees.
///
/// @author Steve Ebersole
public final class ClassificationMigrationValidator {
	/// Validates one baseline-to-current migration.
	public Result validate(
			ClassificationMetadata baseline,
			ClassificationMetadata current,
			JavaMigrationCompatibilityAnalyzer.Analysis javaAnalysis) {
		validateResolved( "Baseline", baseline );
		validateResolved( "Current", current );

		final VersionFamily baselineFamily = VersionFamily.parse( baseline.getHibernateVersion() );
		final VersionFamily currentFamily = VersionFamily.parse( current.getHibernateVersion() );
		if ( currentFamily.compareTo( baselineFamily ) < 0 ) {
			throw new IllegalArgumentException(
					"Current Hibernate family " + current.getHibernateVersion()
							+ " precedes baseline family " + baseline.getHibernateVersion()
			);
		}

		final boolean apiEnforced = baselineFamily.major == currentFamily.major;
		final boolean spiEnforced = baselineFamily.equals( currentFamily );
		final List<Diagnostic> diagnostics = new ArrayList<>();
		validateClassificationChanges( baseline, current, javaAnalysis, apiEnforced, spiEnforced, diagnostics );
		for ( JavaMigrationCompatibilityAnalyzer.Change change : javaAnalysis.getChanges() ) {
			validateJavaChange( baseline, current, change, apiEnforced, spiEnforced, diagnostics );
		}
		diagnostics.sort( Diagnostic.ORDERING );
		return new Result( baseline, current, apiEnforced, spiEnforced, diagnostics );
	}

	private static void validateResolved(String label, ClassificationMetadata metadata) {
		final List<String> unresolved = new ArrayList<>();
		for ( ClassificationModel.Element element : metadata.getModel().getElements() ) {
			if ( metadata.isMigrationCompatibilityElement( element )
					&& element.getClassificationStatus() != RESOLVED ) {
				unresolved.add( element.getId() + '[' + element.getClassificationStatus().name() + ']' );
			}
		}
		if ( !unresolved.isEmpty() ) {
			throw new IllegalArgumentException(
					label + " classification metadata contains " + unresolved.size()
							+ " unresolved declarations; first is " + unresolved.get( 0 )
			);
		}
	}

	private static void validateClassificationChanges(
			ClassificationMetadata baseline,
			ClassificationMetadata current,
			JavaMigrationCompatibilityAnalyzer.Analysis javaAnalysis,
			boolean apiEnforced,
			boolean spiEnforced,
			Collection<Diagnostic> diagnostics) {
		for ( ClassificationModel.Element oldElement : baseline.getModel().getElements() ) {
			if ( !baseline.isMigrationCompatibilityElement( oldElement )
					|| oldElement.getKind() == PACKAGE
					|| oldElement.getLifecycle().isIncubating() ) {
				continue;
			}
			final String currentId = javaAnalysis.getInheritedDeclarations().getOrDefault( oldElement.getId(), oldElement.getId() );
			final ClassificationModel.Element newElement = current.getModel().getElement( currentId );
			if ( newElement == null ) {
				continue;
			}
			if ( apiEnforced && oldElement.getCategory() == API && newElement.getCategory() != API ) {
				diagnostics.add(
						Diagnostic.classificationChange(
								oldElement,
								newElement,
								Surface.API,
								Collections.emptySet(),
								"API classification was removed"
						)
				);
			}
			if ( spiEnforced && oldElement.getCategory() == SPI ) {
				if ( newElement.getCategory() != SPI ) {
					diagnostics.add(
							Diagnostic.classificationChange(
									oldElement,
									newElement,
									Surface.SPI,
									oldElement.getEffectiveRoles(),
									"SPI classification was removed"
							)
				);
				}
				else {
					for ( ClassificationModel.Role role : oldElement.getEffectiveRoles() ) {
						if ( !newElement.getEffectiveRoles().contains( role ) ) {
							diagnostics.add(
									Diagnostic.roleRemoval( oldElement, newElement, role )
							);
						}
					}
				}
			}
		}
	}

	private static void validateJavaChange(
			ClassificationMetadata baseline,
			ClassificationMetadata current,
			JavaMigrationCompatibilityAnalyzer.Change change,
			boolean apiEnforced,
			boolean spiEnforced,
			Collection<Diagnostic> diagnostics) {
		final ClassificationModel.Element oldElement = subject( baseline.getModel(), change );
		if ( oldElement == null
				|| !baseline.isMigrationCompatibilityElement( oldElement )
				|| oldElement.getLifecycle().isIncubating() ) {
			return;
		}
		final ClassificationModel.Element newSubject = subject( current.getModel(), change );

		if ( apiEnforced && oldElement.getCategory() == API ) {
			diagnostics.add(
					Diagnostic.javaChange(
							change,
							oldElement,
							newSubject,
							Surface.API,
							Collections.emptySet()
					)
			);
		}
		if ( spiEnforced && oldElement.getCategory() == SPI ) {
			for ( ClassificationModel.Role role : oldElement.getEffectiveRoles() ) {
				if ( appliesToRole( change.getCause(), role ) ) {
					diagnostics.add(
							Diagnostic.javaChange(
									change,
									oldElement,
									newSubject,
									Surface.SPI,
									EnumSet.of( role )
							)
					);
				}
			}
		}
	}

	private static ClassificationModel.Element subject(
			ClassificationModel model,
			JavaMigrationCompatibilityAnalyzer.Change change) {
		final ClassificationModel.Element exact = model.getElement( change.getElementId() );
		return exact == null && ownerBasedCause( change.getCause() )
				? model.getElement( change.getOwnerId() )
				: exact;
	}

	private static boolean ownerBasedCause(Cause cause) {
		return cause == Cause.ABSTRACT_METHOD_ADDED
				|| cause == Cause.DEFAULT_METHOD_ADDED
				|| cause == Cause.OVERLOAD_ADDED
				|| cause == Cause.ENUM_CONSTANT_ADDED;
	}

	private static boolean appliesToRole(Cause cause, ClassificationModel.Role role) {
		if ( commonLinkageCause( cause ) ) {
			return true;
		}
		if ( role == USE ) {
			return useCause( cause );
		}
		if ( role == IMPLEMENT ) {
			return implementCause( cause );
		}
		return role == SUPPLY && supplyCause( cause );
	}

	private static boolean commonLinkageCause(Cause cause) {
		switch ( cause ) {
			case TYPE_REMOVED:
			case TYPE_KIND_CHANGED:
			case TYPE_VISIBILITY_REDUCED:
			case SUPERCLASS_CHANGED:
			case INTERFACE_REMOVED:
			case GENERIC_SIGNATURE_CHANGED:
			case FIELD_REMOVED:
			case FIELD_VISIBILITY_REDUCED:
			case FIELD_TYPE_CHANGED:
			case FIELD_STATIC_CHANGED:
			case CONSTRUCTOR_REMOVED:
			case CONSTRUCTOR_VISIBILITY_REDUCED:
			case METHOD_REMOVED:
			case METHOD_VISIBILITY_REDUCED:
			case METHOD_RETURN_TYPE_CHANGED:
			case METHOD_STATIC_CHANGED:
				return true;
			default:
				return false;
		}
	}

	private static boolean useCause(Cause cause) {
		switch ( cause ) {
			case TYPE_BECAME_ABSTRACT:
			case RECORD_COMPONENTS_CHANGED:
			case ENUM_CONSTANT_REMOVED:
			case ENUM_CONSTANT_ADDED:
			case ENUM_CONSTANTS_REORDERED:
			case FIELD_BECAME_FINAL:
			case CONSTANT_VALUE_CHANGED:
			case OVERLOAD_ADDED:
			case VARARGS_CHANGED:
			case DECLARED_EXCEPTION_ADDED:
			case DECLARED_EXCEPTION_REMOVED:
			case ANNOTATION_DEFAULT_CHANGED:
				return true;
			default:
				return false;
		}
	}

	private static boolean implementCause(Cause cause) {
		switch ( cause ) {
			case TYPE_BECAME_FINAL:
			case TYPE_BECAME_ABSTRACT:
			case TYPE_BECAME_SEALED:
			case SUPERCLASS_CHANGED:
			case INTERFACE_REMOVED:
			case INTERFACE_ADDED:
			case FIELD_BECAME_FINAL:
			case METHOD_BECAME_FINAL:
			case METHOD_BECAME_ABSTRACT:
			case ABSTRACT_METHOD_ADDED:
			case DEFAULT_METHOD_ADDED:
			case VARARGS_CHANGED:
			case DECLARED_EXCEPTION_ADDED:
			case DECLARED_EXCEPTION_REMOVED:
				return true;
			default:
				return false;
		}
	}

	private static boolean supplyCause(Cause cause) {
		switch ( cause ) {
			case RECORD_COMPONENTS_CHANGED:
			case VARARGS_CHANGED:
			case DECLARED_EXCEPTION_ADDED:
			case DECLARED_EXCEPTION_REMOVED:
			case ANNOTATION_DEFAULT_CHANGED:
				return true;
			default:
				return false;
		}
	}

	/// The compatibility surface whose release horizon made a diagnostic
	/// actionable.
	public enum Surface {
		API,
		SPI
	}

	/// Whether a diagnostic came from classification drift, SPI-role drift, or
	/// a Java declaration change.
	public enum FindingCause {
		CLASSIFICATION_REMOVED,
		SPI_ROLE_REMOVED,
		JAVA_CHANGE
	}

	/// Validation severity. Potential context-dependent breaks remain visible
	/// for review without failing the comparison by themselves.
	public enum Severity {
		ERROR,
		REVIEW
	}

	/// One deterministic migration-compatibility diagnostic.
	public static final class Diagnostic {
		private static final Comparator<Diagnostic> ORDERING = Comparator
				.comparing( Diagnostic::getSurface )
				.thenComparing( Diagnostic::getElementId )
				.thenComparing( Diagnostic::getFindingCause )
				.thenComparing( diagnostic -> diagnostic.javaCause == null ? "" : diagnostic.javaCause.name() )
				.thenComparing( diagnostic -> diagnostic.roles.toString() );

		private final Surface surface;
		private final FindingCause findingCause;
		private final Cause javaCause;
		private final Severity severity;
		private final String elementId;
		private final ClassificationModel.Category baselineCategory;
		private final ClassificationModel.Category currentCategory;
		private final Set<ClassificationModel.Role> roles;
		private final Set<JavaMigrationCompatibilityAnalyzer.Impact> impacts;
		private final String message;

		private Diagnostic(
				Surface surface,
				FindingCause findingCause,
				Cause javaCause,
				Severity severity,
				String elementId,
				ClassificationModel.Category baselineCategory,
				ClassificationModel.Category currentCategory,
				Collection<ClassificationModel.Role> roles,
				Collection<JavaMigrationCompatibilityAnalyzer.Impact> impacts,
				String message) {
			this.surface = surface;
			this.findingCause = findingCause;
			this.javaCause = javaCause;
			this.severity = severity;
			this.elementId = elementId;
			this.baselineCategory = baselineCategory;
			this.currentCategory = currentCategory;
			this.roles = roles.isEmpty()
					? Collections.emptySet()
					: Collections.unmodifiableSet( EnumSet.copyOf( roles ) );
			this.impacts = impacts.isEmpty()
					? Collections.emptySet()
					: Collections.unmodifiableSet( EnumSet.copyOf( impacts ) );
			this.message = message;
		}

		private static Diagnostic classificationChange(
				ClassificationModel.Element baseline,
				ClassificationModel.Element current,
				Surface surface,
				Collection<ClassificationModel.Role> roles,
				String message) {
			return new Diagnostic(
					surface,
					FindingCause.CLASSIFICATION_REMOVED,
					null,
					Severity.ERROR,
					baseline.getId(),
					baseline.getCategory(),
					current.getCategory(),
					roles,
					EnumSet.of( JavaMigrationCompatibilityAnalyzer.Impact.BINARY, JavaMigrationCompatibilityAnalyzer.Impact.SOURCE ),
					message
			);
		}

		private static Diagnostic roleRemoval(
				ClassificationModel.Element baseline,
				ClassificationModel.Element current,
				ClassificationModel.Role role) {
			return new Diagnostic(
					Surface.SPI,
					FindingCause.SPI_ROLE_REMOVED,
					null,
					Severity.ERROR,
					baseline.getId(),
					baseline.getCategory(),
					current.getCategory(),
					EnumSet.of( role ),
					EnumSet.of( JavaMigrationCompatibilityAnalyzer.Impact.BINARY, JavaMigrationCompatibilityAnalyzer.Impact.SOURCE ),
					"SPI role " + role + " was removed"
			);
		}

		private static Diagnostic javaChange(
				JavaMigrationCompatibilityAnalyzer.Change change,
				ClassificationModel.Element baseline,
				ClassificationModel.Element current,
				Surface surface,
				Collection<ClassificationModel.Role> roles) {
			return new Diagnostic(
					surface,
					FindingCause.JAVA_CHANGE,
					change.getCause(),
					change.getCertainty() == JavaMigrationCompatibilityAnalyzer.Certainty.DEFINITE
							? Severity.ERROR
							: Severity.REVIEW,
					change.getElementId(),
					baseline.getCategory(),
					current == null ? null : current.getCategory(),
					roles,
					change.getImpacts(),
					change.getCause().name() + ": " + value( change.getBaselineValue() ) + " -> " + value( change.getCurrentValue() )
			);
		}

		private static String value(String value) {
			return value == null ? "absent" : value;
		}

		public Surface getSurface() {
			return surface;
		}

		public FindingCause getFindingCause() {
			return findingCause;
		}

		public Cause getJavaCause() {
			return javaCause;
		}

		public Severity getSeverity() {
			return severity;
		}

		public String getElementId() {
			return elementId;
		}

		public ClassificationModel.Category getBaselineCategory() {
			return baselineCategory;
		}

		public ClassificationModel.Category getCurrentCategory() {
			return currentCategory;
		}

		public Set<ClassificationModel.Role> getRoles() {
			return roles;
		}

		public Set<JavaMigrationCompatibilityAnalyzer.Impact> getImpacts() {
			return impacts;
		}

		public String getMessage() {
			return message;
		}
	}

	/// The complete policy decision and its diagnostics.
	public static final class Result {
		private final ClassificationMetadata baseline;
		private final ClassificationMetadata current;
		private final boolean apiEnforced;
		private final boolean spiEnforced;
		private final List<Diagnostic> diagnostics;

		private Result(
				ClassificationMetadata baseline,
				ClassificationMetadata current,
				boolean apiEnforced,
				boolean spiEnforced,
				Collection<Diagnostic> diagnostics) {
			this.baseline = baseline;
			this.current = current;
			this.apiEnforced = apiEnforced;
			this.spiEnforced = spiEnforced;
			this.diagnostics = Collections.unmodifiableList( new ArrayList<>( diagnostics ) );
		}

		public ClassificationMetadata getBaseline() {
			return baseline;
		}

		public ClassificationMetadata getCurrent() {
			return current;
		}

		public boolean isApiEnforced() {
			return apiEnforced;
		}

		public boolean isSpiEnforced() {
			return spiEnforced;
		}

		public List<Diagnostic> getDiagnostics() {
			return diagnostics;
		}

		public boolean hasFailures() {
			return diagnostics.stream().anyMatch( diagnostic -> diagnostic.getSeverity() == Severity.ERROR );
		}
	}

	private static final class VersionFamily implements Comparable<VersionFamily> {
		private final int major;
		private final int minor;

		private VersionFamily(int major, int minor) {
			this.major = major;
			this.minor = minor;
		}

		private static VersionFamily parse(String version) {
			final String[] parts = version.split( "\\." );
			if ( parts.length != 2 ) {
				throw new IllegalArgumentException( "Hibernate compatibility family must have form X.Y: " + version );
			}
			try {
				return new VersionFamily( Integer.parseInt( parts[0] ), Integer.parseInt( parts[1] ) );
			}
			catch (NumberFormatException e) {
				throw new IllegalArgumentException( "Hibernate compatibility family must have form X.Y: " + version, e );
			}
		}

		@Override
		public int compareTo(VersionFamily other) {
			final int majorComparison = Integer.compare( major, other.major );
			return majorComparison == 0 ? Integer.compare( minor, other.minor ) : majorComparison;
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof VersionFamily && compareTo( (VersionFamily) other ) == 0;
		}

		@Override
		public int hashCode() {
			return 31 * major + minor;
		}
	}
}
