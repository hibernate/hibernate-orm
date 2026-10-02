package org.hibernate.query.results.internal.complete;

import org.hibernate.metamodel.mapping.EntityValuedModelPart;

/**
 * @author Steve Ebersole
 */
public interface ModelPartReferenceEntity extends ModelPartReference {
	@Override
	EntityValuedModelPart getReferencedPart();
}
