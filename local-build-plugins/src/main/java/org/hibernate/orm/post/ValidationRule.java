/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

/// Stable classification and SPI validation rule identifiers.
///
/// @author Steve Ebersole
public enum ValidationRule {
	CLS001( Domain.CLASSIFICATION, Severity.ERROR, "Remove the conflicting category evidence so the declaration resolves to exactly one of API, SPI, or INTERNAL." ),
	CLS002( Domain.CLASSIFICATION, Severity.ERROR, "Replace the forbidden signature dependency with a contract allowed by the category dependency matrix." ),
	CLS003( Domain.CLASSIFICATION, Severity.ERROR, "Classify the declaration from an appropriate category root or remove the invalid exposure path." ),
	CLS004( Domain.CLASSIFICATION, Severity.ERROR, "Classify the externally accessible declaration as API, SPI, or INTERNAL." ),
	CLS005( Domain.CLASSIFICATION, Severity.ERROR, "Change the consumer dependency to a contract supported for that consumer audience." ),
	SPI001( Domain.SPI, Severity.ERROR, "Declare at least one role and use only roles valid for the declaration target." ),
	SPI002( Domain.SPI, Severity.ERROR, "Make the implementation point externally implementable or remove IMPLEMENT." ),
	SPI003( Domain.SPI, Severity.WARNING, "Classify the external override point IMPLEMENT or make it non-overridable/internal." ),
	SPI004( Domain.SPI, Severity.ERROR, "Restore compatibility or explicitly update the SPI baseline and Migration Guide." ),
	SPI005( Domain.SPI, Severity.WARNING, "Document registration, ownership, reuse, thread safety, lifecycle, multiplicity, and failures." );

	private final Domain domain;
	private final Severity severity;
	private final String remediation;

	ValidationRule(Domain domain, Severity severity, String remediation) {
		this.domain = domain;
		this.severity = severity;
		this.remediation = remediation;
	}

	public String getId() {
		return name();
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
