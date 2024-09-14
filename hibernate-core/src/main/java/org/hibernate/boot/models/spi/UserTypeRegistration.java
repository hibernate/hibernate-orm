/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models.spi;

import org.hibernate.models.spi.ClassDetails;

/**
 * Registration for a {@linkplain org.hibernate.usertype.UserType}
 *
 * @see org.hibernate.annotations.TypeRegistration
 *
 * @author Steve Ebersole
 */
public class UserTypeRegistration {
	private final ClassDetails domainClass;
	private final ClassDetails userTypeClass;

	public UserTypeRegistration(ClassDetails domainClass, ClassDetails userTypeClass) {
		this.domainClass = domainClass;
		this.userTypeClass = userTypeClass;
	}

	public ClassDetails getDomainClass() {
		return domainClass;
	}

	public ClassDetails getUserTypeClass() {
		return userTypeClass;
	}
}
