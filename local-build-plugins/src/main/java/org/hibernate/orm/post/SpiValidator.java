/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

/// Applies the SPI validation rules to the canonical model and to explicit
/// source, provider-boundary, and compatibility evidence.
///
/// @author Steve Ebersole
public final class SpiValidator {
	/// Validates compiled structure and any supplied external evidence.
	public Result validate(
			SpiModel model,
			Evidence evidence,
			SpiValidationAllowlist allowlist) {
		final List<Diagnostic> diagnostics = new ArrayList<>();
		validateCompiledRules( model, diagnostics );
		validateEvidenceRules( model, evidence, diagnostics );
		diagnostics.sort( Diagnostic.ORDERING );

		final Set<SpiValidationAllowlist.Entry> usedEntries = new HashSet<>();
		for ( Diagnostic diagnostic : diagnostics ) {
			final SpiValidationAllowlist.Entry match = allowlist.find( diagnostic.rule, diagnostic.elementId );
			if ( match != null ) {
				diagnostic.allowlistMatch = match;
				usedEntries.add( match );
			}
		}

		final List<String> configurationErrors = new ArrayList<>();
		for ( SpiValidationAllowlist.Entry entry : allowlist.getEntries() ) {
			if ( !usedEntries.contains( entry ) ) {
				configurationErrors.add( "Unused SPI validation allowlist entry: " + entry );
			}
		}
		return new Result( diagnostics, configurationErrors );
	}

	private static void validateCompiledRules(SpiModel model, List<Diagnostic> diagnostics) {
		for ( SpiModel.Element element : model.getElements() ) {
			if ( element.getClassification() == SpiModel.Classification.INDEPENDENT ) {
				if ( element.getLifecycle().isInternal() ) {
					diagnostics.add( diagnostic(
							Rule.SPI001,
							element,
							element.getEffectiveRoles(),
							singletonPath( element ),
							"The element is both effectively SPI and effectively internal"
					) );
				}
				if ( isInternalPackage( element.getDeclaringPackage() ) ) {
					diagnostics.add( diagnostic(
							Rule.SPI002,
							element,
							element.getEffectiveRoles(),
							singletonPath( element ),
							"The SPI element is declared in an internal package"
					) );
				}
				validateRoleTarget( element, diagnostics );
				validateImplementationPoint( model, element, diagnostics );
				if ( isInternalElementInExactSpiPackage( element ) ) {
					diagnostics.add( diagnostic(
							Rule.SPI007,
							element,
							element.getEffectiveRoles(),
							singletonPath( element ),
							"An externally accessible internal type is declared directly in an exact .spi package"
					) );
				}
			}

			if ( element.getLifecycle().isInternal() ) {
				final SpiModel.ReachabilityPath path = shortestSignaturePath( element );
				if ( path != null ) {
					final Set<SpiModel.Role> roles = rootRoles( model, path );
					diagnostics.add( diagnostic(
							Rule.SPI003,
							element,
							roles,
							path.getElementIds(),
							"A supported SPI signature requires an internal type"
					) );
				}
			}
		}
	}

	private static void validateRoleTarget(SpiModel.Element element, List<Diagnostic> diagnostics) {
		if ( element.getEffectiveRoles().isEmpty() ) {
			diagnostics.add( diagnostic(
					Rule.SPI004,
					element,
					Collections.emptySet(),
					singletonPath( element ),
					"The SPI declaration has an explicitly empty role array"
			) );
			return;
		}
		if ( element.getKind() == SpiModel.ElementKind.FIELD
				&& element.getDeclaredRoles().contains( SpiModel.Role.IMPLEMENT ) ) {
			diagnostics.add( diagnostic(
					Rule.SPI004,
					element,
					EnumSet.of( SpiModel.Role.IMPLEMENT ),
					singletonPath( element ),
					"IMPLEMENT is not valid on a field"
			) );
		}
		if ( element.getKind() == SpiModel.ElementKind.CONSTRUCTOR
				&& element.getDeclaredRoles().contains( SpiModel.Role.SUPPLY ) ) {
			diagnostics.add( diagnostic(
					Rule.SPI004,
					element,
					EnumSet.of( SpiModel.Role.SUPPLY ),
					singletonPath( element ),
					"SUPPLY is not valid on a constructor"
			) );
		}
	}

	private static void validateImplementationPoint(
			SpiModel model,
			SpiModel.Element element,
			List<Diagnostic> diagnostics) {
		if ( !element.getEffectiveRoles().contains( SpiModel.Role.IMPLEMENT ) ) {
			return;
		}
		if ( element.getKind() == SpiModel.ElementKind.TYPE
				&& !element.getStructure().isInterfaceType() ) {
			if ( element.getStructure().isFinal() ) {
				diagnostics.add( diagnostic(
						Rule.SPI005,
						element,
						EnumSet.of( SpiModel.Role.IMPLEMENT ),
						singletonPath( element ),
						"An IMPLEMENT class is final"
				) );
			}
			if ( !hasSupportedSubclassConstructor( model, element ) ) {
				diagnostics.add( diagnostic(
						Rule.SPI005,
						element,
						EnumSet.of( SpiModel.Role.IMPLEMENT ),
						singletonPath( element ),
						"An IMPLEMENT class has no public or protected IMPLEMENT constructor"
				) );
			}
		}
		else if ( element.getKind() == SpiModel.ElementKind.METHOD
				&& hasLocallyScopedImplementOrigin( element )
				&& !element.getStructure().isOverridableMethod() ) {
			diagnostics.add( diagnostic(
					Rule.SPI005,
					element,
					EnumSet.of( SpiModel.Role.IMPLEMENT ),
					singletonPath( element ),
					"An explicitly classified IMPLEMENT method is not overridable"
			) );
		}
	}

	private static boolean hasSupportedSubclassConstructor(SpiModel model, SpiModel.Element type) {
		final String prefix = "constructor:" + type.getId().substring( "type:".length() ) + "#<init>(";
		for ( SpiModel.Element candidate : model.getElements() ) {
			if ( candidate.getId().startsWith( prefix )
					&& candidate.getEffectiveRoles().contains( SpiModel.Role.IMPLEMENT )
					&& candidate.getStructure().isExternallySubclassAccessible() ) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasLocallyScopedImplementOrigin(SpiModel.Element element) {
		for ( SpiModel.Origin origin : element.getOrigins() ) {
			if ( origin.getRoles().contains( SpiModel.Role.IMPLEMENT )
					&& origin.getKind() != SpiModel.OriginKind.ENCLOSING_TYPE ) {
				return true;
			}
		}
		return false;
	}

	private static boolean isInternalElementInExactSpiPackage(SpiModel.Element element) {
		if ( element.getKind() != SpiModel.ElementKind.TYPE
				&& element.getKind() != SpiModel.ElementKind.ANNOTATION_TYPE ) {
			return false;
		}
		if ( !element.getLifecycle().isInternal() ) {
			return false;
		}
		for ( SpiModel.Origin origin : element.getOrigins() ) {
			if ( origin.getKind() == SpiModel.OriginKind.EXACT_SPI_PACKAGE ) {
				return true;
			}
		}
		return false;
	}

	private static void validateEvidenceRules(
			SpiModel model,
			Evidence evidence,
			List<Diagnostic> diagnostics) {
		for ( Rule rule : Arrays.asList( Rule.SPI006, Rule.SPI008, Rule.SPI009, Rule.SPI010 ) ) {
			for ( Evidence.Item item : evidence.items.get( rule ) ) {
				final SpiModel.Element element = model.getElement( item.elementId );
				if ( rule == Rule.SPI010
						&& (element == null || !element.getEffectiveRoles().contains( SpiModel.Role.SUPPLY )) ) {
					continue;
				}
				diagnostics.add(
						new Diagnostic(
								rule,
								item.elementId,
								element == null ? Collections.emptySortedSet() : element.getOrigins(),
								item.roles,
								item.path.isEmpty() ? Collections.singletonList( item.elementId ) : item.path,
								item.message,
								rule.remediation
						)
				);
			}
		}
	}

	private static Diagnostic diagnostic(
			Rule rule,
			SpiModel.Element element,
			Collection<SpiModel.Role> roles,
			List<String> path,
			String message) {
		return new Diagnostic(
				rule,
				element.getId(),
				element.getOrigins(),
				roles,
				path,
				message,
				rule.remediation
		);
	}

	private static List<String> singletonPath(SpiModel.Element element) {
		return Collections.singletonList( element.getId() );
	}

	private static SpiModel.ReachabilityPath shortestSignaturePath(SpiModel.Element element) {
		for ( SpiModel.ReachabilityPath path : element.getReachabilityPaths() ) {
			if ( path.getElementIds().size() > 1 ) {
				return path;
			}
		}
		return null;
	}

	private static Set<SpiModel.Role> rootRoles(SpiModel model, SpiModel.ReachabilityPath path) {
		final SpiModel.Element root = model.getElement( path.getElementIds().get( 0 ) );
		return root == null ? Collections.emptySet() : root.getEffectiveRoles();
	}

	private static boolean isInternalPackage(String packageName) {
		for ( String component : packageName.split( "\\." ) ) {
			if ( "internal".equals( component ) ) {
				return true;
			}
		}
		return false;
	}

	public enum Severity {
		ERROR,
		WARNING
	}

	/// Centrally configured rule identifiers, severities, and remediation.
	public enum Rule {
		SPI001( Severity.ERROR, "Remove either the SPI classification or the effective internal classification." ),
		SPI002( Severity.ERROR, "Move the SPI declaration out of the internal package or remove its SPI classification." ),
		SPI003( Severity.ERROR, "Replace the internal signature type with a supported contract." ),
		SPI004( Severity.ERROR, "Declare at least one role and use only roles valid for the declaration target." ),
		SPI005( Severity.ERROR, "Make the implementation point externally implementable or remove IMPLEMENT." ),
		SPI006( Severity.WARNING, "Classify the external override point IMPLEMENT or make it non-overridable/internal." ),
		SPI007( Severity.ERROR, "Move the internal type outside the exact .spi package or make it supported." ),
		SPI008( Severity.ERROR, "Replace the provider dependency with supported API or SPI." ),
		SPI009( Severity.ERROR, "Restore compatibility or explicitly update the SPI baseline and Migration Guide." ),
		SPI010( Severity.WARNING, "Document registration, ownership, reuse, thread safety, lifecycle, multiplicity, and failures." );

		private final Severity severity;
		private final String remediation;

		Rule(Severity severity, String remediation) {
			this.severity = severity;
			this.remediation = remediation;
		}

		public String getId() {
			return name();
		}

		public Severity getSeverity() {
			return severity;
		}

		public String getRemediation() {
			return remediation;
		}

		public static Rule fromId(String id) {
			return valueOf( id );
		}
	}

	/// Explicit evidence supplied by source analysis, provider-boundary analysis,
	/// or the compatibility comparator.
	public static final class Evidence {
		public static final Evidence NONE = builder().build();

		private final Map<Rule, List<Item>> items;

		private Evidence(Map<Rule, List<Item>> items) {
			this.items = items;
		}

		public static Builder builder() {
			return new Builder();
		}

		public static final class Builder {
			private final Map<Rule, List<Item>> items = new EnumMap<>( Rule.class );

			private Builder() {
				for ( Rule rule : Rule.values() ) {
					items.put( rule, new ArrayList<>() );
				}
			}

			public Builder add(
					Rule rule,
					String elementId,
					Collection<SpiModel.Role> roles,
					Collection<String> path,
					String message) {
				if ( rule != Rule.SPI006 && rule != Rule.SPI008 && rule != Rule.SPI009 && rule != Rule.SPI010 ) {
					throw new IllegalArgumentException( "Rule " + rule + " is derived from compiled metadata" );
				}
				items.get( rule ).add( new Item( elementId, roles, path, message ) );
				return this;
			}

			public Evidence build() {
				final Map<Rule, List<Item>> copy = new EnumMap<>( Rule.class );
				for ( Map.Entry<Rule, List<Item>> entry : items.entrySet() ) {
					final List<Item> values = new ArrayList<>( entry.getValue() );
					values.sort( Comparator.comparing( (item) -> item.elementId ) );
					copy.put( entry.getKey(), Collections.unmodifiableList( values ) );
				}
				return new Evidence( Collections.unmodifiableMap( copy ) );
			}
		}

		private static final class Item {
			private final String elementId;
			private final Set<SpiModel.Role> roles;
			private final List<String> path;
			private final String message;

			private Item(
					String elementId,
					Collection<SpiModel.Role> roles,
					Collection<String> path,
					String message) {
				this.elementId = elementId;
				this.roles = roles.isEmpty()
						? Collections.emptySet()
						: Collections.unmodifiableSet( EnumSet.copyOf( roles ) );
				this.path = Collections.unmodifiableList( new ArrayList<>( path ) );
				this.message = message;
			}
		}
	}

	/// One deterministic rule violation.
	public static final class Diagnostic {
		private static final Comparator<Diagnostic> ORDERING = Comparator
				.comparing( (Diagnostic diagnostic) -> diagnostic.rule.getId() )
				.thenComparing( (diagnostic) -> diagnostic.elementId )
				.thenComparing( (diagnostic) -> diagnostic.message )
				.thenComparing( (diagnostic) -> String.join( " -> ", diagnostic.path ) );

		private final Rule rule;
		private final String elementId;
		private final SortedSet<SpiModel.Origin> origins;
		private final Set<SpiModel.Role> roles;
		private final List<String> path;
		private final String message;
		private final String remediation;
		private SpiValidationAllowlist.Entry allowlistMatch;

		private Diagnostic(
				Rule rule,
				String elementId,
				SortedSet<SpiModel.Origin> origins,
				Collection<SpiModel.Role> roles,
				Collection<String> path,
				String message,
				String remediation) {
			this.rule = rule;
			this.elementId = elementId;
			this.origins = origins;
			this.roles = roles.isEmpty()
					? Collections.emptySet()
					: Collections.unmodifiableSet( EnumSet.copyOf( roles ) );
			this.path = Collections.unmodifiableList( new ArrayList<>( path ) );
			this.message = message;
			this.remediation = remediation;
		}

		public Rule getRule() {
			return rule;
		}

		public Severity getSeverity() {
			return rule.severity;
		}

		public String getElementId() {
			return elementId;
		}

		public SortedSet<SpiModel.Origin> getOrigins() {
			return origins;
		}

		public Set<SpiModel.Role> getRoles() {
			return roles;
		}

		public List<String> getPath() {
			return path;
		}

		public String getMessage() {
			return message;
		}

		public String getRemediation() {
			return remediation;
		}

		public SpiValidationAllowlist.Entry getAllowlistMatch() {
			return allowlistMatch;
		}
	}

	/// Validation diagnostics plus allowlist consistency failures.
	public static final class Result {
		private final List<Diagnostic> diagnostics;
		private final List<String> configurationErrors;

		private Result(List<Diagnostic> diagnostics, List<String> configurationErrors) {
			this.diagnostics = Collections.unmodifiableList( new ArrayList<>( diagnostics ) );
			this.configurationErrors = Collections.unmodifiableList( new ArrayList<>( configurationErrors ) );
		}

		public List<Diagnostic> getDiagnostics() {
			return diagnostics;
		}

		public List<String> getConfigurationErrors() {
			return configurationErrors;
		}

		public boolean hasFailures() {
			if ( !configurationErrors.isEmpty() ) {
				return true;
			}
			for ( Diagnostic diagnostic : diagnostics ) {
				if ( diagnostic.getSeverity() == Severity.ERROR && diagnostic.getAllowlistMatch() == null ) {
					return true;
				}
			}
			return false;
		}
	}
}
