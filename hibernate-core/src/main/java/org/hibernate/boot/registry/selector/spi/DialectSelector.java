/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.registry.selector.spi;

import org.hibernate.boot.registry.selector.internal.LazyServiceResolver;
import org.hibernate.dialect.Dialect;
import org.hibernate.service.JavaServiceLoadable;
import org.hibernate.service.Service;

/**
 * @author Christian Beikov
 */
@JavaServiceLoadable
public interface DialectSelector extends Service, LazyServiceResolver<Dialect> {
}
