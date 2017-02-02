/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal;

import org.hibernate.cfg.SecondPass;

/**
 * Because {@link org.hibernate.boot.model.naming.ImplicitNamingStrategy} often requires
 * access info from PersistentClass, we sometimes need to wait until the proper PersistentClass
 * is bound to the in-flight metadata, which means a SecondPass (in this version still using
 * second passes).
 *
 * @author Steve Ebersole
 */
public interface ImplicitColumnNamingSecondPass extends SecondPass {
}
