/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

/// Renders the compact package, type, and member path format used by the
/// established internal and lifecycle reports.
///
/// @author Steve Ebersole
public final class ClassificationReportRenderer {
	public String render(
			ClassificationModel model,
			Predicate<ClassificationModel.Element> selector,
			String header) {
		final StringBuilder report = new StringBuilder( header ).append( "\n\n" );
		for ( ProjectionEntry entry : project( model, selector ) ) {
			report.append( entry.getPath() );
			if ( entry.isPackage() ) {
				report.append( ".*" );
			}
			report.append( '\n' );
		}
		return report.toString();
	}

	/// Selects canonical records and applies the established report-path
	/// collapsing rules. Exposed for cross-projection contract tests.
	List<ProjectionEntry> project(
			ClassificationModel model,
			Predicate<ClassificationModel.Element> selector) {
		final Map<String, ProjectionEntry> entriesByPath = new TreeMap<>();
		for ( ClassificationModel.Element element : model.getElements() ) {
			if ( selector.test( element ) ) {
				final ProjectionEntry entry = projectionEntry( element );
				entriesByPath.putIfAbsent( entry.getPath(), entry );
			}
		}

		final List<ProjectionEntry> projected = new ArrayList<>();
		String previousPath = null;
		for ( ProjectionEntry entry : entriesByPath.values() ) {
			if ( previousPath == null || !entry.getPath().startsWith( previousPath ) ) {
				projected.add( entry );
				previousPath = entry.getPath();
			}
		}
		return Collections.unmodifiableList( projected );
	}

	private static ProjectionEntry projectionEntry(ClassificationModel.Element element) {
		switch ( element.getKind() ) {
			case PACKAGE:
				return new ProjectionEntry( element.getId(), element.getDeclaringPackage(), true );
			case TYPE:
			case ANNOTATION_TYPE:
				return new ProjectionEntry( element.getId(), afterKind( element.getId() ), false );
			case FIELD:
				return new ProjectionEntry( element.getId(), afterKind( element.getId() ), false );
			case CONSTRUCTOR:
			case METHOD:
				final String memberId = afterKind( element.getId() );
				final int parameters = memberId.indexOf( '(' );
				return new ProjectionEntry(
						element.getId(),
						parameters < 0 ? memberId : memberId.substring( 0, parameters ),
						false
				);
			default:
				throw new IllegalArgumentException( "Unexpected classification element kind " + element.getKind() );
		}
	}

	private static String afterKind(String elementId) {
		return elementId.substring( elementId.indexOf( ':' ) + 1 );
	}

	static final class ProjectionEntry {
		private final String elementId;
		private final String path;
		private final boolean packageEntry;

		private ProjectionEntry(String elementId, String path, boolean packageEntry) {
			this.elementId = elementId;
			this.path = path;
			this.packageEntry = packageEntry;
		}

		String getElementId() {
			return elementId;
		}

		String getPath() {
			return path;
		}

		boolean isPackage() {
			return packageEntry;
		}
	}
}
