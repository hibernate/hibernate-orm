/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi;

import org.hibernate.boot.internal.MetadataImpl;
import org.hibernate.service.Service;

/**
 * Allows bootstrapping Hibernate ORM using a custom {@link SessionFactoryBuilderImplementor}
 */
public interface SessionFactoryBuilderService extends Service {

	SessionFactoryBuilderImplementor createSessionFactoryBuilder(MetadataImpl metadata, BootstrapContext bootstrapContext);

}
