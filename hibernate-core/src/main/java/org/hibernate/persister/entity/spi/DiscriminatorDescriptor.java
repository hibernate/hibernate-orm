/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity.spi;

import org.hibernate.persister.common.spi.TypeExporter;
import org.hibernate.persister.common.spi.SingularOrmAttribute;
import org.hibernate.persister.common.spi.VirtualAttribute;

/**
 * Binding of the discriminator in a entity hierarchy
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public interface DiscriminatorDescriptor
		extends TypeExporter, VirtualAttribute, SingularOrmAttribute {
}

