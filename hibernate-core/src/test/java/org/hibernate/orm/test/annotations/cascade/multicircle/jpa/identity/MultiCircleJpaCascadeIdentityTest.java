/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cascade.multicircle.jpa.identity;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * This test uses a complicated model that requires Hibernate to delay
 * inserts until non-nullable transient entity dependencies are resolved.
 * <p>
 * All IDs are generated from an identity column.
 * <p>
 * JPA cascade types are used (jakarta.persistence.CascadeType)..
 * <p>
 * This test uses the following model:
 *
 * <code>
 * ------------------------------ N G
 * |
 * |                                1
 * |                                |
 * |                                |
 * |                                N
 * |
 * |         E N--------------0,1 * F
 * |
 * |         1                      N
 * |         |                      |
 * |         |                      |
 * 1         N                      |
 * *                                |
 * B * N---1 D * 1------------------
 * *
 * N         N
 * |         |
 * |         |
 * 1         |
 * |
 * C * 1-----
 * </code>
 * <p>
 * In the diagram, all associations are bidirectional;
 * assocations marked with '*' cascade persist, save, merge operations to the
 * associated entities (e.g., B cascades persist to D, but D does not cascade
 * persist to B);
 * <p>
 * b, c, d, e, f, and g are all transient unsaved that are associated with each other.
 * <p>
 * When saving b, the entities are added to the ActionQueue in the following order:
 * c, d (depends on e), f (depends on d, g), e, b, g.
 * <p>
 * Entities are inserted in the following order:
 * c, e, d, b, g, f.
 */
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsIdentityColumns.class)
@DomainModel(
		annotatedClasses = {
				EntityB.class,
				EntityC.class,
				EntityD.class,
				EntityE.class,
				EntityF.class,
				EntityG.class
		})
@SessionFactory
public class MultiCircleJpaCascadeIdentityTest {
	private EntityB b;
	private EntityC c;
	private EntityD d;
	private EntityE e;
	private EntityF f;
	private EntityG g;

	@BeforeEach
	public void setup(SessionFactoryScope scope) {
		b = new EntityB();
		c = new EntityC();
		d = new EntityD();
		e = new EntityE();
		f = new EntityF();
		g = new EntityG();

		b.getGCollection().add( g );
		b.setC( c );
		b.setD( d );

		c.getBCollection().add( b );
		c.getDCollection().add( d );

		d.getBCollection().add( b );
		d.setC( c );
		d.setE( e );
		d.getFCollection().add( f );

		e.getDCollection().add( d );
		e.setF( f );

		f.getECollection().add( e );
		f.setD( d );
		f.setG( g );

		g.setB( b );
		g.getFCollection().add( f );
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testPersist(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.persist( b )

		);
		check( scope );
	}

	@Test
	public void testMerge(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					b = (EntityB) session.merge( b );
					c = b.getC();
					d = b.getD();
					e = d.getE();
					f = e.getF();
					g = f.getG();
				}
		);
		check( scope );
	}

	private void check(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityB bRead = session.get( EntityB.class, b.getId() );
					assertEquals( b, bRead );

					EntityG gRead = bRead.getGCollection().iterator().next();
					assertEquals( g, gRead );
					EntityC cRead = bRead.getC();
					assertEquals( c, cRead );
					EntityD dRead = bRead.getD();
					assertEquals( d, dRead );

					assertSame( bRead, cRead.getBCollection().iterator().next() );
					assertSame( dRead, cRead.getDCollection().iterator().next() );

					assertSame( bRead, dRead.getBCollection().iterator().next() );
					assertEquals( cRead, dRead.getC() );
					EntityE eRead = dRead.getE();
					assertEquals( e, eRead );
					EntityF fRead = dRead.getFCollection().iterator().next();
					assertEquals( f, fRead );

					assertSame( dRead, eRead.getDCollection().iterator().next() );
					assertSame( fRead, eRead.getF() );

					assertSame( eRead, fRead.getECollection().iterator().next() );
					assertSame( dRead, fRead.getD() );
					assertSame( gRead, fRead.getG() );

					assertSame( bRead, gRead.getB() );
					assertSame( fRead, gRead.getFCollection().iterator().next() );
				}
		);
	}

}
