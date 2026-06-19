/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.extension;

import java.lang.annotation.Annotation;

/// Internal rehearsal of the future binding-layer attribute extension shape.
///
/// This is not a supported extension contract.  It lets built-in annotations use
/// the same capability-oriented target style expected from a future replacement
/// for `org.hibernate.binder.AttributeBinder`, while ORM still adapts those
/// contributions to current compatibility materialization internally.
///
/// @since 9.0
/// @author Steve Ebersole
public interface AttributeBindingContributor<A extends Annotation> {
	void contribute(
			A annotation,
			AttributeBindingTarget target,
			BindingContributionContext context);
}
