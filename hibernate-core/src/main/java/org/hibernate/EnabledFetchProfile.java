/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import jakarta.persistence.FindOption;

/**
 * A {@link jakarta.persistence.FindOption} which requests a named
 * {@linkplain org.hibernate.annotations.FetchProfile fetch profile}.
 *
 * @param profileName the {@link org.hibernate.annotations.FetchProfile#name()}
 *
 * @since 7.0
 *
 * @see org.hibernate.annotations.FetchProfile
 * @see Session#find(Class, Object, FindOption...)
 *
 * @author Gavin King
 */
public record EnabledFetchProfile(String profileName)
		implements FindOption {
}
