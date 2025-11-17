/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.metamodel.CollectionClassification;

/**
 * Descriptor for the collection identifier.  Only used with {@link CollectionClassification#ID_BAG} collections
 *
 * @author Steve Ebersole
 */
public interface CollectionIdentifierDescriptor extends CollectionPart, BasicValuedModelPart {
}
