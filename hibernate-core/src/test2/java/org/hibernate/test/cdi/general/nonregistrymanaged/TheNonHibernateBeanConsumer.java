/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.cdi.general.nonregistrymanaged;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

/**
 * Simulates CDI bean consumers outside of Hibernate ORM
 * that rely on the same beans used in Hibernate ORM.
 *
 * Allows us to assert that consumed beans can be shared between Hibernate ORM consumers
 * and non-Hibernate consumers.
 *
 * @author Yoann Rodiere
 */
@Singleton
public class TheNonHibernateBeanConsumer {
	public static final String NAME = "TheAlternativeNamedApplicationScopedBeanImpl_name";

	@javax.inject.Inject
	private TheSharedApplicationScopedBean sharedApplicationScopedBean;

	@PostConstruct
	public void ensureInstancesInitialized() {
		sharedApplicationScopedBean.ensureInitialized();
	}
}
