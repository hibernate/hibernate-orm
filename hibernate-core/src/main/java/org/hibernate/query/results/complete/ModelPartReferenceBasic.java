/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.complete;

import org.hibernate.metamodel.mapping.BasicValuedModelPart;

/**
 * @author Steve Ebersole
 */
public interface ModelPartReferenceBasic extends ModelPartReference {
	@Override
	BasicValuedModelPart getReferencedPart();
}
