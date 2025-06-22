/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.mutability.converted;

import org.hibernate.annotations.Mutability;
import org.hibernate.type.descriptor.java.Immutability;

/**
 * @author Steve Ebersole
 */
@Mutability(Immutability.class)
public class ImmutabilityDateConverter extends DateConverter {
}
