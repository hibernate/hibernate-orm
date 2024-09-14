/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi;

import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.type.CompositeType;

/**
 * @author Steve Ebersole
 */
public interface CompositeTypeImplementor extends CompositeType {
	void injectMappingModelPart(EmbeddableValuedModelPart part, MappingModelCreationProcess process);
	EmbeddableValuedModelPart getMappingModelPart();
}
