/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cascade.circle.identity;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
@DomainModel(
		annotatedClasses = {
				A.class,
				B.class,
				C.class,
				D.class,
				E.class,
				F.class,
				G.class,
				H.class
		}
)
@SessionFactory
public class CascadeCircleIdentityIdTest {

	@Test
	@JiraKey(value = "HHH-5472")
	public void testCascade(SessionFactoryScope scope) {
		A a = new A();
		B b = new B();
		C c = new C();
		D d = new D();
		E e = new E();
		F f = new F();
		G g = new G();
		H h = new H();

		a.getBCollection().add( b );
		b.setA( a );

		a.getCCollection().add( c );
		c.setA( a );

		b.getCCollection().add( c );
		c.setB( b );

		a.getDCollection().add( d );
		d.getACollection().add( a );

		d.getECollection().add( e );
		e.setF( f );

		f.getBCollection().add( b );
		b.setF( f );

		c.setG( g );
		g.getCCollection().add( c );

		f.setH( h );
		h.setG( g );

		scope.inTransaction(
				session -> {
					// Fails: says that C.b is null (even though it isn't). Doesn't fail if you persist c, g or h instead of a
					session.persist( a );
					session.flush();
				}
		);
	}

}
