/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.domain;

import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.EmbeddedContainer;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;

/**
 * Models an embeddable-valued mapping such as an {@link javax.persistence.Embedded} or
 * a {@link javax.persistence.ElementCollection}
 *
 * @author Steve Ebersole
 */
public interface EmbeddedValueMapping extends ValueMapping, ManagedTypeMapping {
	<X> EmbeddedTypeDescriptor<X> makeRuntimeDescriptor(
			EmbeddedContainer embeddedContainer,
			String localName,
			RuntimeModelCreationContext context);
}
