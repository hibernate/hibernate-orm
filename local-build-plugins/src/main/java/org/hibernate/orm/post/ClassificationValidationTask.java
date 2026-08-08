/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

/// Validates category, dependency, reachability, and consumer-boundary rules
/// from canonical classification metadata.
///
/// @author Steve Ebersole
public abstract class ClassificationValidationTask extends AbstractClassificationValidationTask {
	public ClassificationValidationTask() {
		super( "Validates Hibernate API, SPI, and internal classifications", "classification-validation.txt" );
	}

	@Override
	protected ValidationResult validate(ClassificationModel model, ValidationAllowlist allowlist) {
		return new ClassificationValidator().validate( model, ValidationEvidence.NONE, allowlist );
	}

	@Override
	protected String title() {
		return "Hibernate ORM classification validation";
	}
}
