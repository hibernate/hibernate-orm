/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.usertype;

import org.hibernate.Incubating;
import org.hibernate.annotations.Type;
import org.hibernate.models.spi.MemberDetails;

import java.util.Properties;

/**
 * Access to information useful during {@linkplain UserType} creation and initialization.
 *
 * @author Yanming Zhou
 * @see AnnotationBasedUserType
 *
 * @since 7.3
 */
@Incubating(since = "7.3")
public interface UserTypeCreationContext {
	/**
	 * Access to the {@link MemberDetails}.
	 */
	MemberDetails getMemberDetails();

	/**
	 * Access to the parameters.
	 *
	 * @see Type#parameters()
	 */
	Properties getParameters();

}
