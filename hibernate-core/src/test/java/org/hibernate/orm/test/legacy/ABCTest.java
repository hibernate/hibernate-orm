/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;

import java.util.Iterator;
import java.util.List;

import org.hibernate.Cache;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.Table;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(
		xmlMappings = {
				"org/hibernate/orm/test/legacy/ABC.hbm.xml", "org/hibernate/orm/test/legacy/ABCExtends.hbm.xml"
		}
)
@SessionFactory
public class ABCTest {

	@Test
	public void testFormulaAssociation(SessionFactoryScope scope) {
		Long did = 12L;
		scope.inTransaction(
				session -> {
					D d = new D( did );
					session.persist( d );
					A a = new A();
					a.setName( "a" );
					session.persist( a );
					d.setReverse( a );
					d.inverse = a;
				}
		);

		scope.inTransaction(
				session -> {
					D d = session.get( D.class, did );
					assertNotNull( d.getReverse() );
					session.clear();
					getCache( scope ).evictEntityData( D.class );
					getCache( scope ).evictEntityData( A.class );
					d = session.get( D.class, did );
					assertNotNull( d.inverse );
					assertThat( d.inverse.getName(), is( "a" ) );
					session.clear();
					getCache( scope ).evictEntityData( D.class );
					getCache( scope ).evictEntityData( A.class );
					assertThat(
							session.createQuery( "from D d join d.reverse r join d.inverse i where i = r", Object[].class )
									.list().size(),
							is( 1 )
					);
				}
		);
	}

	@Test
	public void testHigherLevelIndexDefinition(SessionFactoryScope scope) {
		Table table = scope.getMetadataImplementor()
				.getDatabase()
				.getDefaultNamespace()
				.locateTable( Identifier.toIdentifier( "TA" ) );
		Iterator<Index> indexItr = table.getIndexes().values().iterator();
		boolean found = false;
		while ( indexItr.hasNext() ) {
			final Index index = indexItr.next();
			if ( "indx_a_name".equals( index.getName() ) ) {
				found = true;
				break;
			}
		}
		assertTrue( found, "Unable to locate indx_a_name index" );
	}

	@Test
	public void testSubclassing(SessionFactoryScope scope) {
		C1 c = new C1();
		scope.inTransaction(
				session -> {
					D d = new D();
					d.setAmount( 213.34f );
					c.setAddress( "foo bar" );
					c.setCount( 23432 );
					c.setName( "c1" );
					c.setBName( "a funny name" );
					c.setD( d );
					session.persist( c );
					d.setId( c.getId() );
					session.persist( d );

					assertThat( session.createQuery( "from C2 c where 1=1 or 1=1" ).list().size(), is( 0 ) );
				}
		);

		getCache( scope ).evictEntityData( A.class );

		scope.inTransaction(
				session -> {
					C1 c1 = (C1) session.get( A.class, c.getId() );
					assertTrue(
							c1.getAddress().equals( "foo bar" ) &&
									( c1.getCount() == 23432 ) &&
									c1.getName().equals( "c1" ) &&
									c1.getD().getAmount() > 213.3f
					);
					assertThat( c1.getBName(), is( "a funny name" ) );
				}
		);

		getCache( scope ).evictEntityData( A.class );

		scope.inTransaction(
				session -> {
					C1 c1 = (C1) session.get( B.class, c.getId() );
					assertTrue(
							c1.getAddress().equals( "foo bar" ) &&
									( c1.getCount() == 23432 ) &&
									c1.getName().equals( "c1" ) &&
									c1.getD().getAmount() > 213.3f
					);
					assertThat( c1.getBName(), is( "a funny name" ) );
				}
		);

		scope.inTransaction(
				session -> {
					C1 c1 = session.getReference( C1.class, c.getId() );
					assertTrue(
							c1.getAddress().equals( "foo bar" ) &&
									( c1.getCount() == 23432 ) &&
									c1.getName().equals( "c1" ) &&
									c1.getD().getAmount() > 213.3f
					);
				}
		);

		scope.inTransaction(
				session -> {
					List<C1> bs = session.createQuery( "from B" ).list();
					for ( C1 b : bs ) {
						session.remove( b );
						session.remove( b.getD() );
					}
				}
		);
	}

	@Test
	public void testGetSave(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					assertNull( session.get( D.class, 1L ) );
					D d = new D();
					d.setId( 1L );
					session.persist( d );
					session.flush();
					assertNotNull( session.get( D.class, 1L ) );
					session.remove( d );
					session.flush();
				}
		);
	}

	private Cache getCache(SessionFactoryScope scope) {
		return scope.getSessionFactory().getCache();
	}

}
