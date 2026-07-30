/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.usertype;

import org.hibernate.Incubating;
import org.hibernate.Remove;
import org.hibernate.annotations.Type;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.service.ServiceRegistry;

import java.util.Properties;

/**
 * Access to information useful during {@linkplain UserType} creation and initialization.
 *
 * @author Yanming Zhou
 * @see AnnotationBasedUserType
 *
 * @since 7.3
 */
@Incubating
public interface UserTypeCreationContext {
	/**
	 * Access to the {@link MetadataBuildingContext}.
	 *
	 * @apiNote This broad access will be removed in 9.0. Users requiring
	 * specific boot-time values should report those requirements.
	 */
	@Remove
	MetadataBuildingContext getBuildingContext();

	/**
	 * Access to available services.
	 *
	 * @apiNote This broad access will be removed in 9.0. Users requiring
	 * specific services should report those requirements.
	 */
	@Remove
	ServiceRegistry getServiceRegistry();

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
