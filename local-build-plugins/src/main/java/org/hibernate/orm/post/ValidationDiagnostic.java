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

/// One deterministic classification or SPI validation diagnostic.
///
/// @author Steve Ebersole
public final class ValidationDiagnostic {
	static final Comparator<ValidationDiagnostic> ORDERING = Comparator
			.comparing( (ValidationDiagnostic diagnostic) -> diagnostic.cause.name() )
			.thenComparing( ValidationDiagnostic::getSourceElementId )
			.thenComparing( ValidationDiagnostic::getTargetElementId )
			.thenComparing( ValidationDiagnostic::getEdgeKind )
			.thenComparing( ValidationDiagnostic::getMessage );

	private final ValidationCause cause;
	private final String sourceElementId;
	private final String targetElementId;
	private final ClassificationModel.Category sourceCategory;
	private final ClassificationModel.Category targetCategory;
	private final String edgeKind;
	private final Set<ClassificationModel.Role> roles;
	private final List<String> path;
	private final String message;
	private ValidationAllowlist.Entry allowlistMatch;

	ValidationDiagnostic(
			ValidationCause cause,
			String sourceElementId,
			String targetElementId,
			ClassificationModel.Category sourceCategory,
			ClassificationModel.Category targetCategory,
			String edgeKind,
			Collection<ClassificationModel.Role> roles,
			Collection<String> path,
			String message) {
		this.cause = cause;
		this.sourceElementId = sourceElementId;
		this.targetElementId = targetElementId;
		this.sourceCategory = sourceCategory;
		this.targetCategory = targetCategory;
		this.edgeKind = edgeKind;
		this.roles = roles.isEmpty()
				? Collections.emptySet()
				: Collections.unmodifiableSet( EnumSet.copyOf( roles ) );
		this.path = Collections.unmodifiableList( new ArrayList<>( path ) );
		this.message = message;
	}

	public ValidationCause getCause() {
		return cause;
	}

	public ValidationCause.Severity getSeverity() {
		return cause.getSeverity();
	}

	public String getElementId() {
		return sourceElementId;
	}

	public String getSourceElementId() {
		return sourceElementId;
	}

	public String getTargetElementId() {
		return targetElementId;
	}

	public ClassificationModel.Category getSourceCategory() {
		return sourceCategory;
	}

	public ClassificationModel.Category getTargetCategory() {
		return targetCategory;
	}

	public String getEdgeKind() {
		return edgeKind;
	}

	public Set<ClassificationModel.Role> getRoles() {
		return roles;
	}

	public List<String> getPath() {
		return path;
	}

	public String getMessage() {
		return message;
	}

	public String getRemediation() {
		return cause.getRemediation();
	}

	public ValidationAllowlist.Entry getAllowlistMatch() {
		return allowlistMatch;
	}

	void setAllowlistMatch(ValidationAllowlist.Entry allowlistMatch) {
		this.allowlistMatch = allowlistMatch;
	}
}
