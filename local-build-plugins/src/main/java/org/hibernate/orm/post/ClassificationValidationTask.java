/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.tasks.Classpath;

/// Validates category, dependency, reachability, and consumer-boundary rules
/// from canonical classification metadata.
///
/// @author Steve Ebersole
public abstract class ClassificationValidationTask extends AbstractClassificationValidationTask {
	private final ConfigurableFileCollection providerArtifacts;

	public ClassificationValidationTask() {
		super( "Validates Hibernate API, SPI, and internal classifications", "classification-validation.txt" );
		providerArtifacts = getProject().getObjects().fileCollection();
	}

	@Classpath
	public ConfigurableFileCollection getProviderArtifacts() {
		return providerArtifacts;
	}

	@Override
	protected ValidationResult validate(ClassificationModel model, ValidationAllowlist allowlist) {
		return new ClassificationValidator().validate(
				model,
				allowlist,
				ClassificationValidationScope.withProviderArtifacts( providerArtifacts.getFiles() )
		);
	}

	@Override
	protected String title() {
		return "Hibernate ORM classification validation";
	}
}
