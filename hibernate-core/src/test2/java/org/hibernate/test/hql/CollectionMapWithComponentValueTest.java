/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

/**
 * @author Andrea Boriero
 */

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import org.junit.After;
import org.junit.Test;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * @author Andrea Boriero
 */
public class CollectionMapWithComponentValueTest extends BaseCoreFunctionalTestCase {
	private final KeyValue keyValue = new KeyValue();

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				TestEntity.class,
				KeyValue.class
		};
	}

	@Override
	protected void prepareTest() throws Exception {
		doInHibernate( this::sessionFactory, s -> {
			s.save( keyValue );

			TestEntity testEntity = new TestEntity();
			EmbeddableValue embeddableValue = new EmbeddableValue();
			embeddableValue.value = 3;
			Map<KeyValue, EmbeddableValue> map = new HashMap<>();
			map.put( keyValue, embeddableValue );
			testEntity.values = map;
			s.save( testEntity );

			KeyValue keyValue2 = new KeyValue();
			s.save( keyValue2 );
			TestEntity testEntity2 = new TestEntity();
			EmbeddableValue embeddableValue2 = new EmbeddableValue();
			embeddableValue2.value = 3;
			Map<KeyValue, EmbeddableValue> map2 = new HashMap<>();
			map.put( keyValue2, embeddableValue2 );
			testEntity2.values = map2;
			s.save( testEntity2 );
		} );
	}

	@Test
	public void testMapKeyExpressionInWhere() {
		doInHibernate( this::sessionFactory, s -> {
			// JPA form
			Query query = s.createQuery(
					"select te from TestEntity te join te.values v where ? in (key(v)) " );
			query.setParameter( 0, keyValue );

			assertThat( query.list().size(), is( 1 ) );

			// Hibernate additional form
			query = s.createQuery( "select te from TestEntity te where ? in (key(te.values))" );
			query.setParameter( 0, keyValue );

			assertThat( query.list().size(), is( 1 ) );
		} );
	}

	@Test
	public void testMapKeyExpressionInSelect() {
		doInHibernate( this::sessionFactory, s -> {
			// JPA form
			List results = s.createQuery( "select key(v) from TestEntity te join te.values v" ).list();
			assertEquals( 2, results.size() );
			assertTyping( KeyValue.class, results.get( 0 ) );

			// Hibernate additional form
			results = s.createQuery( "select key(te.values) from TestEntity te" ).list();
			assertEquals( 2, results.size() );
			assertTyping( KeyValue.class, results.get( 0 ) );
		} );
	}

	@Test
	public void testMapValueExpressionInSelect() {
		doInHibernate( this::sessionFactory, s -> {
			List addresses = s.createQuery( "select value(v) from TestEntity te join te.values v" ).list();
			assertEquals( 2, addresses.size() );
			assertTyping( EmbeddableValue.class, addresses.get( 0 ) );

			addresses = s.createQuery( "select value(te.values) from TestEntity te" ).list();
			assertEquals( 2, addresses.size() );
			assertTyping( EmbeddableValue.class, addresses.get( 0 ) );
		} );
	}

	@Test
	public void testMapEntryExpressionInSelect() {
		doInHibernate( this::sessionFactory, s -> {
			List addresses = s.createQuery( "select entry(v) from TestEntity te join te.values v" ).list();
			assertEquals( 2, addresses.size() );
			assertTyping( Map.Entry.class, addresses.get( 0 ) );

			addresses = s.createQuery( "select entry(te.values) from TestEntity te" ).list();
			assertEquals( 2, addresses.size() );
			assertTyping( Map.Entry.class, addresses.get( 0 ) );
		} );
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Entity(name = "TestEntity")
	@Table(name = "TEST_ENTITY")
	public static class TestEntity {
		@Id
		@GeneratedValue
		Long id;

		@ElementCollection
		Map<KeyValue, EmbeddableValue> values;
	}

	@Entity(name = "KeyValue")
	@Table(name = "KEY_VALUE")
	public static class KeyValue {
		@Id
		@GeneratedValue
		Long id;
	}

	@Embeddable
	public static class EmbeddableValue {
		Integer value;
	}
}
