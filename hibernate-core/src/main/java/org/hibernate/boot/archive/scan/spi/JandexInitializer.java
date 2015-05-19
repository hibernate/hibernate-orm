/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.archive.scan.spi;

import org.jboss.jandex.ClassInfo;

/**
 * Helper for preparing Jandex for later use..
 *
 * Not currently used.  See https://hibernate.atlassian.net/browse/HHH-9489
 *
 * @author Steve Ebersole
 */
public interface JandexInitializer {
	ClassInfo handle(PackageDescriptor packageDescriptor);

	ClassInfo handle(ClassDescriptor classDescriptor);
}
