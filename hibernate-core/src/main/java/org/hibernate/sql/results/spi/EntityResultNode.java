/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.query.NavigablePath;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Represents a reference to an entity either as a return, fetch, or collection element or index.
 *
 * @author Steve Ebersole
 */
public interface EntityResultNode extends ResultSetMappingNode, FetchParent {
	@Override
	NavigablePath getNavigablePath();

	EntityValuedModelPart getEntityValuedModelPart();

	@Override
	default JavaTypeDescriptor getResultJavaTypeDescriptor() {
		return getEntityValuedModelPart().getEntityMappingType().getMappedJavaTypeDescriptor();
	}

	@Override
	default EntityValuedModelPart getReferencedMappingContainer() {
		return getEntityValuedModelPart();
	}
}
