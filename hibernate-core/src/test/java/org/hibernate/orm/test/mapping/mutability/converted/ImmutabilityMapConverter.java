/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.mutability.converted;

import org.hibernate.annotations.Mutability;
import org.hibernate.type.descriptor.java.Immutability;

/**
 * @author Steve Ebersole
 */
@Mutability(Immutability.class)
public class ImmutabilityMapConverter extends MapConverter {
}
