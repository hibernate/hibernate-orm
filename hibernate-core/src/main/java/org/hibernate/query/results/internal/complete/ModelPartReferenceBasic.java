package org.hibernate.query.results.internal.complete;

import org.hibernate.metamodel.mapping.BasicValuedModelPart;

/**
 * @author Steve Ebersole
 */
public interface ModelPartReferenceBasic extends ModelPartReference {
	@Override
	BasicValuedModelPart getReferencedPart();
}
