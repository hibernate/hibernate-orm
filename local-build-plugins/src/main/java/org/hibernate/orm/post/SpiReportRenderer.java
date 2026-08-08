/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.hibernate.orm.post.ClassificationModel.Category.SPI;
import static org.hibernate.orm.post.ClassificationModel.ClassificationStatus.RESOLVED;
import static org.hibernate.orm.post.ClassificationModel.ElementKind.ANNOTATION_TYPE;
import static org.hibernate.orm.post.ClassificationModel.ElementKind.PACKAGE;
import static org.hibernate.orm.post.ClassificationModel.ElementKind.TYPE;
import static org.hibernate.orm.post.ClassificationModel.Role.IMPLEMENT;
import static org.hibernate.orm.post.ClassificationModel.Role.SUPPLY;
import static org.hibernate.orm.post.ClassificationModel.Role.USE;

/// Renders the concise provider-facing projection of the canonical
/// classification metadata.
///
/// @author Steve Ebersole
public final class SpiReportRenderer {
	private static final List<RoleBucket> ROLE_BUCKETS = Collections.unmodifiableList(
			Arrays.asList(
					new RoleBucket( "USE", USE ),
					new RoleBucket( "IMPLEMENT", IMPLEMENT ),
					new RoleBucket( "SUPPLY", SUPPLY ),
					new RoleBucket( "USE + IMPLEMENT", USE, IMPLEMENT ),
					new RoleBucket( "USE + SUPPLY", USE, SUPPLY ),
					new RoleBucket( "IMPLEMENT + SUPPLY", IMPLEMENT, SUPPLY ),
					new RoleBucket( "USE + IMPLEMENT + SUPPLY", USE, IMPLEMENT, SUPPLY )
			)
	);

	/// Renders the SPI projection with the versions carried by its source
	/// classification document.
	public String render(ClassificationMetadata metadata) {
		final ReportView view = new ReportView( metadata.getModel() );
		final StringBuilder report = new StringBuilder();
		report.append( "= Hibernate ORM Service Provider Interface\n\n" )
				.append( "Compatibility version: `" )
				.append( escapeAsciiDoc( metadata.getHibernateVersion() ) )
				.append( "`\n\n" )
				.append( "Source version: `" )
				.append( escapeAsciiDoc( metadata.getSourceVersion() ) )
				.append( "`\n\n" )
				.append( "Audience: external Hibernate SPI providers. " )
				.append( "Provider `USE` does not make a declaration application-user API.\n\n" );

		for ( RoleBucket bucket : ROLE_BUCKETS ) {
			report.append( "== " ).append( bucket.label ).append( "\n\n" );
			final Map<String, List<ClassificationModel.Element>> packages = view.elementsByBucket.get( bucket );
			if ( packages.isEmpty() ) {
				report.append( "_No declarations._\n\n" );
				continue;
			}
			for ( Map.Entry<String, List<ClassificationModel.Element>> packageEntry : packages.entrySet() ) {
				report.append( "=== `" ).append( escapeAsciiDoc( packageEntry.getKey() ) ).append( "`\n\n" );
				for ( ClassificationModel.Element element : packageEntry.getValue() ) {
					report.append( "* `" ).append( escapeAsciiDoc( displayName( element ) ) ).append( "`\n" );
				}
				report.append( '\n' );
			}
		}

		while ( report.length() > 0 && report.charAt( report.length() - 1 ) == '\n' ) {
			report.setLength( report.length() - 1 );
		}
		return report.append( '\n' ).toString();
	}

	private static boolean isVisibleDeclaration(
			ClassificationModel.Element element,
			ClassificationModel model) {
		if ( element.getClassificationStatus() != RESOLVED
				|| element.getCategory() != SPI
				|| element.getEffectiveRoles().isEmpty() ) {
			return false;
		}
		if ( element.getKind() == PACKAGE || isType( element ) ) {
			return true;
		}
		if ( !element.getDeclaredRoles().isEmpty() ) {
			return true;
		}
		final ClassificationModel.Element owner = model.getElement( element.getOwnerId() );
		return owner == null || !element.getEffectiveRoles().equals( owner.getEffectiveRoles() );
	}

	private static boolean isType(ClassificationModel.Element element) {
		return element.getKind() == TYPE || element.getKind() == ANNOTATION_TYPE;
	}

	private static String displayName(ClassificationModel.Element element) {
		if ( isType( element ) ) {
			return relativeTypeName( element.getId(), element.getDeclaringPackage() );
		}
		final int memberSeparator = element.getId().indexOf( '#' );
		if ( memberSeparator < 0 ) {
			return element.getSignature();
		}
		final String typeName = relativeTypeName(
				"type:" + element.getId().substring( element.getId().indexOf( ':' ) + 1, memberSeparator ),
				element.getDeclaringPackage()
		);
		final String member = element.getId().substring( memberSeparator + 1 );
		if ( member.startsWith( "<init>" ) ) {
			return typeName + member.substring( "<init>".length() );
		}
		return typeName + '#' + member;
	}

	private static String relativeTypeName(String typeId, String packageName) {
		final String className = typeId.substring( "type:".length() );
		final String packagePrefix = packageName.isEmpty() ? "" : packageName + '.';
		final String relative = className.startsWith( packagePrefix )
				? className.substring( packagePrefix.length() )
				: className;
		return relative.replace( '$', '.' );
	}

	private static String escapeAsciiDoc(String text) {
		return text.replace( "`", "&#96;" );
	}

	private static RoleBucket roleBucket(Set<ClassificationModel.Role> roles) {
		for ( RoleBucket bucket : ROLE_BUCKETS ) {
			if ( bucket.roles.equals( roles ) ) {
				return bucket;
			}
		}
		return null;
	}

	private static final class ReportView {
		private static final Comparator<ClassificationModel.Element> ELEMENT_ORDER = Comparator
				.<ClassificationModel.Element>comparingInt( (element) -> isType( element ) ? 0 : 1 )
				.thenComparing( SpiReportRenderer::displayName )
				.thenComparing( ClassificationModel.Element::getId );

		private final Map<RoleBucket, Map<String, List<ClassificationModel.Element>>> elementsByBucket =
				new LinkedHashMap<>();

		private ReportView(ClassificationModel model) {
			for ( RoleBucket bucket : ROLE_BUCKETS ) {
				elementsByBucket.put( bucket, new TreeMap<>() );
			}
			for ( ClassificationModel.Element element : model.getElements() ) {
				if ( !isVisibleDeclaration( element, model ) ) {
					continue;
				}
				final RoleBucket bucket = roleBucket( element.getEffectiveRoles() );
				if ( bucket == null ) {
					throw new IllegalArgumentException(
							"SPI element has no valid effective-role bucket: " + element.getId()
					);
				}
				final Map<String, List<ClassificationModel.Element>> packages = elementsByBucket.get( bucket );
				final List<ClassificationModel.Element> packageElements = packages.computeIfAbsent(
						element.getDeclaringPackage(),
						(key) -> new ArrayList<>()
				);
				if ( element.getKind() != PACKAGE ) {
					packageElements.add( element );
				}
			}
			for ( Map<String, List<ClassificationModel.Element>> packages : elementsByBucket.values() ) {
				for ( List<ClassificationModel.Element> elements : packages.values() ) {
					elements.sort( ELEMENT_ORDER );
				}
			}
		}
	}

	private static final class RoleBucket {
		private final String label;
		private final Set<ClassificationModel.Role> roles;

		private RoleBucket(String label, ClassificationModel.Role... roles) {
			this.label = label;
			this.roles = Collections.unmodifiableSet( EnumSet.copyOf( Arrays.asList( roles ) ) );
		}
	}
}
