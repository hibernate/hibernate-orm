/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.complete;

import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;

/**
 * @author Steve Ebersole
 */
public interface ModelPartReferenceEmbeddable extends ModelPartReference {
	@Override
	EmbeddableValuedModelPart getReferencedPart();
}
