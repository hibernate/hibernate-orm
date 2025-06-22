/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.complete;

import org.hibernate.metamodel.mapping.EntityValuedModelPart;

/**
 * @author Steve Ebersole
 */
public interface ModelPartReferenceEntity extends ModelPartReference {
	@Override
	EntityValuedModelPart getReferencedPart();
}
