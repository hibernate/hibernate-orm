/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity.spi;

import org.hibernate.persister.common.spi.AttributeContainer;
import org.hibernate.persister.common.spi.SingularAttribute;

/**
 * Information about the entity (hierarchy wide) version
 *
 * @author Steve Ebersole
 */
public interface VersionDescriptor extends SingularAttribute {
	@Override
	EntityPersister getAttributeContainer();

	/**
	 * Access to the value that indicates an unsaved (transient) entity
	 *
	 * @return The unsaved value
	 */
	String getUnsavedValue();

	// todo : add a Comparator here?  This would be useful for byte[] (TSQL ROWVERSION types) based versions.  But how would we inject the right Comparator?

}
