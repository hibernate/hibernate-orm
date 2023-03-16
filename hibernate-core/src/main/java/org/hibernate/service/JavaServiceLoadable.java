/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.service;

/**
 * Marker annotation identifying integration points which Hibernate supports loading as a
 * {@linkplain java.util.ServiceLoader Java service}.
 *
 * @see org.hibernate.boot.registry.classloading.spi.ClassLoaderService#loadJavaServices
 *
 * @author Steve Ebersole
 */
public @interface JavaServiceLoadable {
}
