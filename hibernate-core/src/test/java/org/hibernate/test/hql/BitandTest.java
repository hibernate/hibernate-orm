/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

import org.hibernate.testing.SkipLog;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * @author Gail Badner
 */

public class BitandTest extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	public void testBitandColumn() {
		if ( getDialect().getFunctions().get( "bitand" ) == null ) {
			SkipLog.reportSkip( "Dialect does not support bitand function" );
			return;
		}
		doInHibernate( this::sessionFactory, session -> {
			AnEntity anEntity = new AnEntity();
			anEntity.bitand = "A String";
			anEntity.anInt = 5;
			session.persist( anEntity );
			session.flush();

			Object result = session.createQuery( "select bitand, bitand( 1, anInt ) from AnEntity" ).uniqueResult();
			Object[] resultArray = (Object[]) result;
			assertEquals( "A String", resultArray[0] );
			assertEquals( 1L, ( (Number) resultArray[1] ).longValue() );
		});
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { AnEntity.class };
	}

	@Entity(name = "AnEntity")
	public static class AnEntity {

		@Id
		@GeneratedValue
		private int id;

		private String bitand;
		private int anInt;
	}
}
