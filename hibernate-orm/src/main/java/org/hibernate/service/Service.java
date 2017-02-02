/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.service;

import java.io.Serializable;

/**
 * Marker interface for services.
 * <p/>
 * NOTE : All services must be {@link Serializable}!
 *
 * @author Steve Ebersole
 */
public interface Service extends Serializable {
}
