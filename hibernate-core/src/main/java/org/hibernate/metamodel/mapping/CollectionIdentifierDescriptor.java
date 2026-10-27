package org.hibernate.metamodel.mapping;

import org.hibernate.metamodel.CollectionClassification;

/**
 * Descriptor for the collection identifier.  Only used with {@link CollectionClassification#ID_BAG} collections
 *
 * @author Steve Ebersole
 */
public interface CollectionIdentifierDescriptor<J, C> extends CollectionPart<J, J, C>, BasicValuedModelPart<J, J, C> {
}
