/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.type.descriptor.java;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.NaturalId;
import org.hibernate.query.Query;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Test;

/**
 * Tests for implicit widening coercions
 *
 * @author Steve Ebersole
 */
public class CoercionTests  extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				CoercionTests.TheEntity.class
		};
	}

	@Test
	public void testLoading() {
		TransactionUtil.doInHibernate(
				this::sessionFactory, session -> {
					session.byId( TheEntity.class ).load( 1L );

					session.byId( TheEntity.class ).load( (byte) 1 );
					session.byId( TheEntity.class ).load( (short) 1 );
					session.byId( TheEntity.class ).load( 1 );

					session.byId( TheEntity.class ).load( 1.0 );
					session.byId( TheEntity.class ).load( 1.0F );

					session.byId( TheEntity.class ).load( BigInteger.ONE );
					session.byId( TheEntity.class ).load( BigDecimal.ONE );
				}
		);
		TransactionUtil.doInHibernate(
				this::sessionFactory,
				session -> {
					session.byId( TheEntity.class ).getReference( 1L );
					session.byId( TheEntity.class ).getReference( 1 );
				}
		);
	}

	@Test
	public void testMultiIdLoading() {
		TransactionUtil.doInHibernate(
				this::sessionFactory,
				session -> {
					session.byMultipleIds( TheEntity.class ).multiLoad( 1L );

					session.byMultipleIds( TheEntity.class ).multiLoad( (byte) 1 );
					session.byMultipleIds( TheEntity.class ).multiLoad( (short) 1 );
					session.byMultipleIds( TheEntity.class ).multiLoad( 1 );

					session.byMultipleIds( TheEntity.class ).multiLoad( 1.0 );
					session.byMultipleIds( TheEntity.class ).multiLoad( 1.0F );

					session.byMultipleIds( TheEntity.class ).multiLoad( BigInteger.ONE );
					session.byMultipleIds( TheEntity.class ).multiLoad( BigDecimal.ONE );
				}
		);
		TransactionUtil.doInHibernate(
				this::sessionFactory,
				session -> {
					session.byMultipleIds( TheEntity.class ).multiLoad( Arrays.asList( 1L ) );
					session.byMultipleIds( TheEntity.class ).multiLoad( Arrays.asList( 1 ) );
				}
		);
	}

	@Test
	public void testNaturalIdLoading() {
		TransactionUtil.doInHibernate(
				this::sessionFactory,
				session -> {
					session.bySimpleNaturalId( TheEntity.class ).load( 1L );

					session.bySimpleNaturalId( TheEntity.class ).load( (byte) 1 );
					session.bySimpleNaturalId( TheEntity.class ).load( (short) 1 );
					session.bySimpleNaturalId( TheEntity.class ).load( 1 );

					session.bySimpleNaturalId( TheEntity.class ).load( 1.0 );
					session.bySimpleNaturalId( TheEntity.class ).load( 1.0F );

					session.bySimpleNaturalId( TheEntity.class ).load( BigInteger.ONE );
					session.bySimpleNaturalId( TheEntity.class ).load( BigDecimal.ONE );
				}
		);
		TransactionUtil.doInHibernate(
				this::sessionFactory,
				session -> {
					session.byMultipleIds( TheEntity.class ).multiLoad( Arrays.asList( 1L ) );
					session.byMultipleIds( TheEntity.class ).multiLoad( Arrays.asList( 1 ) );
				}
		);
	}

	@Test
	public void testQueryParameterIntegralWiden() {
		final String qry = "select e from TheEntity e where e.longId = :id";

		TransactionUtil.doInHibernate(
				this::sessionFactory,
				session -> {
					final Query query = session.createQuery( qry );

					query.setParameter( "id", 1L ).list();

					query.setParameter( "id", 1 ).list();
				}
		);
	}

	@Test
	public void testQueryParameterIntegralNarrow() {
		final String qry = "select e from TheEntity e where e.intValue = ?1";

		TransactionUtil.doInHibernate(
				this::sessionFactory,
				session -> {
					final Query query = session.createQuery( qry );

					query.setParameter( 1, 1 ).list();

					query.setParameter( 1, 1L ).list();
				}
		);
	}

	@Test
	public void testQueryParameterFloatingWiden() {
		final String qry = "select e from TheEntity e where e.floatValue = :p";

		TransactionUtil.doInHibernate(
				this::sessionFactory,
				session -> {
					final Query query = session.createQuery( qry );

					query.setParameter( "p", 0.5f ).list();

					query.setParameter( "p", 0.5 ).list();
				}
		);
	}

	@Test
	public void testQueryParameterFloatingNarrow() {
		final String qry = "select e from TheEntity e where e.doubleValue = :p";
		TransactionUtil.doInHibernate(
				this::sessionFactory,
				session -> {
					final Query query = session.createQuery( qry );

					query.setParameter( "p", 0.5 ).list();

					query.setParameter( "p", 0.5f ).list();
				}
		);
	}

	@Entity( name = "TheEntity" )
	@Table( name = "the_entity" )
	public static class TheEntity {
		@Id
		private Long longId;
		@NaturalId
		private Long longNaturalId;
		private Integer intValue;
		private Float floatValue;
		private Double doubleValue;

	}
}
