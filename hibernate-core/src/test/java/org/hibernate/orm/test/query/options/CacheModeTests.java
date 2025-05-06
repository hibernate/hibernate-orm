/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.options;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@DomainModel( standardModels = StandardDomainModel.CONTACTS )
@SessionFactory
public class CacheModeTests {
	@Test
	public void testNullCacheMode(SessionFactoryScope scope) {
		// tests passing null as CacheMode
		scope.inTransaction( (session) -> {
			session.createQuery( "select c from Contact c" )
					.setCacheMode( null )
					.list();
		});
	}
}
