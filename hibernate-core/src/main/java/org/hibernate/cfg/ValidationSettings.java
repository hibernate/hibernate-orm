/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cfg;

/**
 * @author Steve Ebersole
 */
public interface ValidationSettings {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Indicates which {@linkplain jakarta.persistence.ValidationMode form of automatic
	 * validation} is in effect as per the rules defined in JPA 2 section 3.6.1.1.
	 * <p>
	 * See JPA 2 sections 9.4.3 and 8.2.1.8
	 *
	 * @see jakarta.persistence.ValidationMode
	 */
	String JAKARTA_VALIDATION_MODE = "jakarta.persistence.validation.mode";

	/**
	 * Used to pass along any discovered {@link jakarta.validation.ValidatorFactory}.
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyValidatorFactory(Object)
	 */
	String JAKARTA_VALIDATION_FACTORY = "jakarta.persistence.validation.factory";

	/**
	 * Used to coordinate with bean validators.
	 * <p>
	 * See JPA 2 section 8.2.1.9
	 */
	@SuppressWarnings("unused")
	String JAKARTA_PERSIST_VALIDATION_GROUP = "jakarta.persistence.validation.group.pre-persist";

	/**
	 * Used to coordinate with bean validators.
	 * <p>
	 * See JPA 2 section 8.2.1.9
	 */
	@SuppressWarnings("unused")
	String JAKARTA_UPDATE_VALIDATION_GROUP = "jakarta.persistence.validation.group.pre-update";

	/**
	 * Used to coordinate with bean validators.
	 * <p>
	 * See JPA 2 section 8.2.1.9
	 */
	@SuppressWarnings("unused")
	String JAKARTA_REMOVE_VALIDATION_GROUP = "jakarta.persistence.validation.group.pre-remove";


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Hibernate settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Enable nullability checking, raises an exception if an attribute marked as
	 * {@linkplain jakarta.persistence.Basic#optional() not null} is null at runtime.
	 * <p>
	 * Defaults to disabled if Bean Validation is present in the classpath and
	 * annotations are used, or enabled otherwise.
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyNullabilityChecking(boolean)
	 */
	String CHECK_NULLABILITY = "hibernate.check_nullability";


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Legacy JPA settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * @deprecated Use {@link #JAKARTA_VALIDATION_MODE} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String JPA_VALIDATION_MODE = "javax.persistence.validation.mode";

	/**
	 * @deprecated Use {@link #JAKARTA_VALIDATION_FACTORY} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String JPA_VALIDATION_FACTORY = "javax.persistence.validation.factory";

	/**
	 * @deprecated Use {@link #JAKARTA_PERSIST_VALIDATION_GROUP} instead
	 */
	@Deprecated
	String JPA_PERSIST_VALIDATION_GROUP = "javax.persistence.validation.group.pre-persist";

	/**
	 * @deprecated Use {@link #JAKARTA_UPDATE_VALIDATION_GROUP} instead
	 */
	@Deprecated
	String JPA_UPDATE_VALIDATION_GROUP = "javax.persistence.validation.group.pre-update";

	/**
	 * @deprecated Use {@link #JAKARTA_REMOVE_VALIDATION_GROUP} instead
	 */
	@Deprecated
	String JPA_REMOVE_VALIDATION_GROUP = "javax.persistence.validation.group.pre-remove";
}
