/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.metamodel.mapping.SingularAttributeMapping;

/**
 * @author Steve Ebersole
 */
@FunctionalInterface
interface AttributeInclusionChecker {
	boolean include(int position, SingularAttributeMapping attribute);
}
