/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.persister.common.spi;

import java.util.List;

/**
 * DomainDescriptor that can define persistent attributes
 *
 * @author Steve Ebersole
 */
public interface AttributeContainer extends NavigableSource {
	// todo : what else would be useful here?

	AttributeContainer getSuperAttributeContainer();

	List<PersistentAttribute> getNonIdentifierAttributes();
	PersistentAttribute findAttribute(String name);

	// todo : overload this for entity- and collection-valued attributes
	//		but that requires splitting SingularAttributeEntity into interface/impl
	//		and moving the interface into SPI
	List<JoinColumnMapping> resolveJoinColumnMappings(PersistentAttribute persistentAttribute);
}
