/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.beanvalidation;

/// Consolidation of settings affecting Bean Validation integration/activation.
///
/// @since 9.0
/// @author Steve Ebersole
public interface BeanValidationSettings {
	String MODE_PROPERTY = "javax.persistence.validation.mode";
	String JAKARTA_MODE_PROPERTY = "jakarta.persistence.validation.mode";

	String VALIDATE_SUPPLIED_FACTORY_METHOD_NAME = "validateSuppliedFactory";

	String JAKARTA_BV_CHECK_CLASS = "jakarta.validation.ConstraintViolation";
	String ACTIVATOR_CLASS_NAME = "org.hibernate.boot.beanvalidation.TypeSafeActivator";
}
