/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Renders the canonical SPI model in deterministic human-readable and
/// machine-readable forms.
///
/// The report is organized by the complete effective role set. Multi-role
/// elements therefore occur in exactly one bucket. Signature-derived elements
/// occupy their own section and independently classified collaborators remain
/// in their role bucket even when signatures also reach them.
///
/// @author Steve Ebersole
public final class SpiReportRenderer {
	public static final String JSON_SCHEMA = "hibernate-orm-spi-report";
	public static final int JSON_SCHEMA_VERSION = 1;

	private static final List<RoleBucket> ROLE_BUCKETS = Collections.unmodifiableList(
			Arrays.asList(
					new RoleBucket( "USE", SpiModel.Role.USE ),
					new RoleBucket( "IMPLEMENT", SpiModel.Role.IMPLEMENT ),
					new RoleBucket( "SUPPLY", SpiModel.Role.SUPPLY ),
					new RoleBucket( "USE + IMPLEMENT", SpiModel.Role.USE, SpiModel.Role.IMPLEMENT ),
					new RoleBucket( "USE + SUPPLY", SpiModel.Role.USE, SpiModel.Role.SUPPLY ),
					new RoleBucket( "IMPLEMENT + SUPPLY", SpiModel.Role.IMPLEMENT, SpiModel.Role.SUPPLY ),
					new RoleBucket(
							"USE + IMPLEMENT + SUPPLY",
							SpiModel.Role.USE,
							SpiModel.Role.IMPLEMENT,
							SpiModel.Role.SUPPLY
					)
			)
	);

	/// Renders the provider-facing AsciiDoc report.
	public String renderAsciiDoc(SpiModel model) {
		final ReportView view = new ReportView( model );
		final StringBuilder report = new StringBuilder();
		report.append( "= Hibernate ORM Service Provider Interface Report\n" )
				.append( ":toc: left\n\n" )
				.append( "This report describes contracts supported for external Hibernate SPI providers. " )
				.append( "SPI classification does not, by itself, make a contract application-user API.\n\n" )
				.append( "== Independently classified provider SPI\n\n" );

		for ( RoleBucket bucket : ROLE_BUCKETS ) {
			report.append( "=== " ).append( bucket.label ).append( "\n\n" );
			final List<SpiModel.Element> elements = view.independentByBucket.get( bucket );
			if ( elements.isEmpty() ) {
				report.append( "_No elements._\n\n" );
			}
			else {
				for ( SpiModel.Element element : elements ) {
					appendAsciiDocElement( report, element );
				}
			}
		}

		report.append( "== Signature-derived supported surface\n\n" );
		if ( view.signatureDerived.isEmpty() ) {
			report.append( "_No elements._\n" );
		}
		else {
			for ( SpiModel.Element element : view.signatureDerived ) {
				appendAsciiDocElement( report, element );
			}
		}
		if ( report.length() > 1 && report.charAt( report.length() - 2 ) == '\n' ) {
			report.setLength( report.length() - 1 );
		}
		return report.toString();
	}

	/// Renders versioned, deterministic JSON for build tooling and CI diffing.
	public String renderJson(SpiModel model) {
		final ReportView view = new ReportView( model );
		final JsonWriter json = new JsonWriter();
		json.beginObject();
		json.name( "schema" ).value( JSON_SCHEMA );
		json.name( "schemaVersion" ).value( JSON_SCHEMA_VERSION );
		json.name( "audience" ).value( "SPI_PROVIDER" );
		json.name( "independent" ).beginObject();
		for ( RoleBucket bucket : ROLE_BUCKETS ) {
			json.name( bucket.jsonName ).beginArray();
			for ( SpiModel.Element element : view.independentByBucket.get( bucket ) ) {
				appendJsonElement( json, element );
			}
			json.endArray();
		}
		json.endObject();
		json.name( "signatureDerived" ).beginArray();
		for ( SpiModel.Element element : view.signatureDerived ) {
			appendJsonElement( json, element );
		}
		json.endArray();
		json.endObject();
		return json.toString();
	}

	private static void appendAsciiDocElement(StringBuilder report, SpiModel.Element element) {
		report.append( "==== `" ).append( escapeAsciiDoc( element.getId() ) ).append( "`\n\n" )
				.append( "* Declaration kind: `" ).append( element.getKind() ).append( "`\n" )
				.append( "* Declaring package: `" ).append( escapeAsciiDoc( element.getDeclaringPackage() ) ).append( "`\n" )
				.append( "* Signature: `" ).append( escapeAsciiDoc( element.getSignature() ) ).append( "`\n" )
				.append( "* Classification: `" ).append( element.getClassification() ).append( "`\n" )
				.append( "* Application API status: `" ).append( element.getApplicationApiStatus() ).append( "`\n" )
				.append( "* Declared roles: " ).append( formatRoles( element.getDeclaredRoles() ) ).append( "\n" )
				.append( "* Effective roles: " ).append( formatRoles( element.getEffectiveRoles() ) ).append( "\n" )
				.append( "* Lifecycle: internal=`" ).append( element.getLifecycle().isInternal() )
				.append( "`, incubating=`" ).append( element.getLifecycle().isIncubating() )
				.append( "`, deprecated=`" ).append( element.getLifecycle().isDeprecated() ).append( "`\n" )
				.append( "* Replacement: none specified\n" )
				.append( "* Source: `" ).append( escapeAsciiDoc( element.getSource() ) ).append( "`\n" )
				.append( "* Classification origins:\n" );
		if ( element.getOrigins().isEmpty() ) {
			report.append( "** _None; signature-derived._\n" );
		}
		else {
			for ( SpiModel.Origin origin : element.getOrigins() ) {
				report.append( "** `" ).append( origin.getKind() ).append( "` from `" )
						.append( escapeAsciiDoc( origin.getSourceElementId() ) ).append( "` with roles " )
						.append( formatRoles( origin.getRoles() ) ).append( "\n" );
			}
		}
		report.append( "* Supported-signature reachability:\n" );
		if ( element.getReachabilityPaths().isEmpty() ) {
			report.append( "** _None._\n" );
		}
		else {
			for ( SpiModel.ReachabilityPath path : element.getReachabilityPaths() ) {
				report.append( "** `" ).append( escapeAsciiDoc( String.join( " -> ", path.getElementIds() ) ) )
						.append( "`\n" );
			}
		}
		if ( element.getOmittedReachabilityPathCount() > 0 ) {
			report.append( "** _" ).append( element.getOmittedReachabilityPathCount() )
					.append( " additional paths omitted._\n" );
		}
		report.append( '\n' );
	}

	private static void appendJsonElement(JsonWriter json, SpiModel.Element element) {
		json.beginObject();
		json.name( "id" ).value( element.getId() );
		json.name( "kind" ).value( element.getKind().name() );
		json.name( "declaringPackage" ).value( element.getDeclaringPackage() );
		json.name( "signature" ).value( element.getSignature() );
		json.name( "classification" ).value( element.getClassification().name() );
		json.name( "applicationApiStatus" ).value( element.getApplicationApiStatus().name() );
		json.name( "declaredRoles" );
		appendJsonRoles( json, element.getDeclaredRoles() );
		json.name( "effectiveRoles" );
		appendJsonRoles( json, element.getEffectiveRoles() );
		json.name( "origins" ).beginArray();
		for ( SpiModel.Origin origin : element.getOrigins() ) {
			json.beginObject();
			json.name( "kind" ).value( origin.getKind().name() );
			json.name( "sourceElementId" ).value( origin.getSourceElementId() );
			json.name( "roles" );
			appendJsonRoles( json, origin.getRoles() );
			json.endObject();
		}
		json.endArray();
		json.name( "lifecycle" ).beginObject();
		json.name( "internal" ).value( element.getLifecycle().isInternal() );
		json.name( "incubating" ).value( element.getLifecycle().isIncubating() );
		json.name( "deprecated" ).value( element.getLifecycle().isDeprecated() );
		json.name( "replacement" ).nullValue();
		json.endObject();
		json.name( "source" ).value( element.getSource() );
		json.name( "migrationExceptions" ).beginArray();
		for ( String migrationException : element.getMigrationExceptions() ) {
			json.value( migrationException );
		}
		json.endArray();
		json.name( "reachabilityPaths" ).beginArray();
		for ( SpiModel.ReachabilityPath path : element.getReachabilityPaths() ) {
			json.beginArray();
			for ( String elementId : path.getElementIds() ) {
				json.value( elementId );
			}
			json.endArray();
		}
		json.endArray();
		json.name( "omittedReachabilityPathCount" ).value( element.getOmittedReachabilityPathCount() );
		json.endObject();
	}

	private static void appendJsonRoles(JsonWriter json, Collection<SpiModel.Role> roles) {
		json.beginArray();
		for ( SpiModel.Role role : SpiModel.Role.values() ) {
			if ( roles.contains( role ) ) {
				json.value( role.name() );
			}
		}
		json.endArray();
	}

	private static String formatRoles(Set<SpiModel.Role> roles) {
		if ( roles.isEmpty() ) {
			return "_None._";
		}
		final List<String> names = new ArrayList<>();
		for ( SpiModel.Role role : SpiModel.Role.values() ) {
			if ( roles.contains( role ) ) {
				names.add( '`' + role.name() + '`' );
			}
		}
		return String.join( " + ", names );
	}

	private static String escapeAsciiDoc(String text) {
		return text.replace( "`", "&#96;" );
	}

	private static final class ReportView {
		private final Map<RoleBucket, List<SpiModel.Element>> independentByBucket = new LinkedHashMap<>();
		private final List<SpiModel.Element> signatureDerived = new ArrayList<>();

		private ReportView(SpiModel model) {
			for ( RoleBucket bucket : ROLE_BUCKETS ) {
				independentByBucket.put( bucket, new ArrayList<>() );
			}
			for ( SpiModel.Element element : model.getElements() ) {
				if ( element.getClassification() == SpiModel.Classification.SIGNATURE_DERIVED ) {
					signatureDerived.add( element );
					continue;
				}
				final RoleBucket bucket = roleBucket( element.getEffectiveRoles() );
				if ( bucket == null ) {
					throw new IllegalArgumentException(
							"Independently classified SPI element has no valid effective-role bucket: " + element.getId()
					);
				}
				independentByBucket.get( bucket ).add( element );
			}
		}
	}

	private static RoleBucket roleBucket(Set<SpiModel.Role> roles) {
		for ( RoleBucket bucket : ROLE_BUCKETS ) {
			if ( bucket.roles.equals( roles ) ) {
				return bucket;
			}
		}
		return null;
	}

	private static final class RoleBucket {
		private final String label;
		private final String jsonName;
		private final Set<SpiModel.Role> roles;

		private RoleBucket(String label, SpiModel.Role... roles) {
			this.label = label;
			this.jsonName = label.replace( " + ", "_" );
			this.roles = Collections.unmodifiableSet( EnumSet.copyOf( Arrays.asList( roles ) ) );
		}
	}

	private static final class JsonWriter {
		private final StringBuilder json = new StringBuilder();
		private final List<Context> contexts = new ArrayList<>();
		private int indent;
		private boolean namePending;

		private JsonWriter beginObject() {
			beforeValue();
			json.append( '{' );
			contexts.add( new Context( true ) );
			indent++;
			return this;
		}

		private JsonWriter endObject() {
			return end( '}' );
		}

		private JsonWriter beginArray() {
			beforeValue();
			json.append( '[' );
			contexts.add( new Context( false ) );
			indent++;
			return this;
		}

		private JsonWriter endArray() {
			return end( ']' );
		}

		private JsonWriter end(char delimiter) {
			indent--;
			final Context context = contexts.remove( contexts.size() - 1 );
			if ( !context.first ) {
				newline();
			}
			json.append( delimiter );
			namePending = false;
			return this;
		}

		private JsonWriter name(String name) {
			final Context context = current();
			if ( !context.object || namePending ) {
				throw new IllegalStateException( "A JSON name is not valid in the current context" );
			}
			beforeEntry( context );
			appendQuoted( name );
			json.append( ": " );
			namePending = true;
			return this;
		}

		private JsonWriter value(String value) {
			beforeValue();
			appendQuoted( value );
			return this;
		}

		private JsonWriter value(int value) {
			beforeValue();
			json.append( value );
			return this;
		}

		private JsonWriter value(boolean value) {
			beforeValue();
			json.append( value );
			return this;
		}

		private JsonWriter nullValue() {
			beforeValue();
			json.append( "null" );
			return this;
		}

		private void beforeValue() {
			if ( contexts.isEmpty() ) {
				return;
			}
			final Context context = current();
			if ( context.object ) {
				if ( !namePending ) {
					throw new IllegalStateException( "A JSON object value requires a name" );
				}
				namePending = false;
			}
			else {
				beforeEntry( context );
			}
		}

		private void beforeEntry(Context context) {
			if ( !context.first ) {
				json.append( ',' );
			}
			newline();
			context.first = false;
		}

		private Context current() {
			return contexts.get( contexts.size() - 1 );
		}

		private void newline() {
			json.append( '\n' );
			for ( int i = 0; i < indent; i++ ) {
				json.append( "  " );
			}
		}

		private void appendQuoted(String value) {
			json.append( '"' );
			for ( int i = 0; i < value.length(); i++ ) {
				final char character = value.charAt( i );
				switch ( character ) {
					case '"':
						json.append( "\\\"" );
						break;
					case '\\':
						json.append( "\\\\" );
						break;
					case '\b':
						json.append( "\\b" );
						break;
					case '\f':
						json.append( "\\f" );
						break;
					case '\n':
						json.append( "\\n" );
						break;
					case '\r':
						json.append( "\\r" );
						break;
					case '\t':
						json.append( "\\t" );
						break;
					default:
						if ( character < 0x20 ) {
							json.append( String.format( "\\u%04x", (int) character ) );
						}
						else {
							json.append( character );
						}
				}
			}
			json.append( '"' );
		}

		@Override
		public String toString() {
			return json.append( '\n' ).toString();
		}

		private static final class Context {
			private final boolean object;
			private boolean first = true;

			private Context(boolean object) {
				this.object = object;
			}
		}
	}
}
