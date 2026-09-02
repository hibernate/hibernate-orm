/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

/// Semantic causes reported by classification and SPI validation.
///
/// Each cause identifies why a declaration or dependency violates the
/// classification model. Its name is the stable identifier used by reports
/// and validation allowlists.
///
/// @author Steve Ebersole
public enum ValidationCause {
	CONFLICTING_CLASSIFICATION(
			Domain.CLASSIFICATION,
			Severity.ERROR,
			"Remove the conflicting category evidence so the declaration resolves to exactly one of API, SPI, or INTERNAL."
	),
	FORBIDDEN_CATEGORY_DEPENDENCY(
			Domain.CLASSIFICATION,
			Severity.ERROR,
			"Replace the forbidden signature dependency with a contract allowed by the category dependency matrix."
	),
	INVALID_CATEGORY_REACHABILITY(
			Domain.CLASSIFICATION,
			Severity.ERROR,
			"Classify the declaration from an appropriate category root or remove the invalid exposure path."
	),
	UNCLASSIFIED_HIBERNATE_DECLARATION(
			Domain.CLASSIFICATION,
			Severity.ERROR,
			"Classify the retained declaration as API, SPI, or INTERNAL, or remove it from the supported surface."
	),
	INVALID_SPI_ROLE_DECLARATION(
			Domain.SPI,
			Severity.ERROR,
			"Declare at least one role and use only roles valid for the declaration target."
	),
	INVALID_SPI_IMPLEMENTATION_POINT(
			Domain.SPI,
			Severity.ERROR,
			"Make the implementation point externally implementable or remove IMPLEMENT."
	);

	private final Domain domain;
	private final Severity severity;
	private final String remediation;

	ValidationCause(Domain domain, Severity severity, String remediation) {
		this.domain = domain;
		this.severity = severity;
		this.remediation = remediation;
	}

	public Domain getDomain() {
		return domain;
	}

	public Severity getSeverity() {
		return severity;
	}

	public String getRemediation() {
		return remediation;
	}

	public enum Domain {
		CLASSIFICATION,
		SPI
	}

	public enum Severity {
		ERROR,
		WARNING
	}
}
