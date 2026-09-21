/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

/// Column ownership for mapped-superclass declarations before projection into entity mappings.
///
/// @author Steve Ebersole
public final class MappedSuperclassColumnContainer extends ColumnContainer {
	private final String diagnosticLabel;

	public MappedSuperclassColumnContainer(String diagnosticLabel) {
		if ( diagnosticLabel == null || diagnosticLabel.isBlank() ) {
			throw new IllegalArgumentException( "Mapped-superclass diagnostic label must not be blank" );
		}
		this.diagnosticLabel = diagnosticLabel;
	}

	public String getDiagnosticLabel() {
		return diagnosticLabel;
	}

	@Override
	public String toString() {
		return diagnosticLabel;
	}
}
