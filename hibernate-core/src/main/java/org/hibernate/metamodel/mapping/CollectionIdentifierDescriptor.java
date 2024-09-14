/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.metamodel.CollectionClassification;

/**
 * Descriptor for the collection identifier.  Only used with {@link CollectionClassification#ID_BAG} collections
 *
 * @author Steve Ebersole
 */
public interface CollectionIdentifierDescriptor extends CollectionPart, BasicValuedModelPart {
}
