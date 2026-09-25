package org.hibernate.boot.model.source.spi;

import org.hibernate.Remove;

/**
 * @author Gail Badner
 */
@Remove
public interface AssociationSource {

	AttributeSource getAttributeSource();

	/**
	 * Obtain the name of the referenced entity.
	 *
	 * @return The name of the referenced entity
	 */
	String getReferencedEntityName();

	boolean isIgnoreNotFound();

	boolean isMappedBy();
}
