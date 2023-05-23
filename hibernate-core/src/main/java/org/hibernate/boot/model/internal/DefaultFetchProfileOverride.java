/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.internal;

import jakarta.persistence.FetchType;
import org.hibernate.annotations.FetchProfileOverride;
import org.hibernate.mapping.FetchProfile;

import java.lang.annotation.Annotation;

import static jakarta.persistence.FetchType.EAGER;

/**
 * @author Gavin King
 */
class DefaultFetchProfileOverride implements FetchProfileOverride {

	static final FetchProfileOverride INSTANCE = new DefaultFetchProfileOverride();

	@Override
	public org.hibernate.annotations.FetchMode mode() {
		return org.hibernate.annotations.FetchMode.JOIN;
	}

	@Override
	public FetchType fetch() {
		return EAGER;
	}

	@Override
	public String profile() {
		return FetchProfile.HIBERNATE_DEFAULT_PROFILE;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return FetchProfileOverride.class;
	}
}
