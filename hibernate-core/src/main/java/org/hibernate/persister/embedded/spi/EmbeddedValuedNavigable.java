/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.embedded.spi;

import org.hibernate.persister.common.spi.NavigableContainer;
import org.hibernate.persister.queryable.spi.EmbeddedValueExpressableType;
import org.hibernate.type.descriptor.java.spi.EmbeddableJavaDescriptor;

/**
 * Describes parts of the domain model that can be composite values.
 *
 * @author Steve Ebersole
 */
public interface EmbeddedValuedNavigable<J> extends EmbeddedValueExpressableType<J>, NavigableContainer<J> {
	@Override
	EmbeddedContainer getContainer();

	EmbeddedPersister<J> getEmbeddedPersister();

	@Override
	EmbeddableJavaDescriptor<J> getJavaTypeDescriptor();
}
