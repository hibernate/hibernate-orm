/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi.descriptor.java.managed;

import javax.persistence.metamodel.IdentifiableType;

/**
 * Internal extension contract for what JPA's IdentifiableType, which is
 * really just saying a ManagedType that may contain identifier and version
 * metadata: aka an EntityType or a MappedSuperclassType.
 *
 * @author Steve Ebersole
 */
public interface JavaTypeDescriptorIdentifiableImplementor
		extends JavaTypeDescriptorManagedImplementor, IdentifiableType {
	@Override
	JavaTypeDescriptorIdentifiableImplementor getSupertype();

	EntityHierarchy getEntityHierarchy();
}
