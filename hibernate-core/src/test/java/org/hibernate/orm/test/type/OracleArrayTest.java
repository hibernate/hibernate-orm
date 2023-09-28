/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.type;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.OracleDialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

@RequiresDialect( OracleDialect.class )
@TestForIssue( jiraKey = "HHH-10999")
public class OracleArrayTest extends BaseCoreFunctionalTestCase {

	@Override
	protected void configure(Configuration configuration) {
		// Make sure this stuff runs on a dedicated connection pool,
		// otherwise we might run into ORA-21700: object does not exist or is marked for delete
		// because the JDBC connection or database session caches something that should have been invalidated
		configuration.setProperty( AvailableSettings.CONNECTION_PROVIDER, "" );
	}

	@Test
	public void test() {
		doInHibernate( this::sessionFactory, session -> {
			ArrayHolder expected = new ArrayHolder( 1, new Integer[] { 1, 2, 3 }, new String[] { "abc", "def" } );
			session.persist( expected );
			session.flush();
			session.clear();

			ArrayHolder arrayHolder = session.find( ArrayHolder.class, 1 );
			Assert.assertEquals( expected.getIntArray(), arrayHolder.getIntArray() );
			Assert.assertEquals( expected.getTextArray(), arrayHolder.getTextArray() );
		} );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				ArrayHolder.class
		};
	}

	@Entity(name = "ArrayHolder")
	public static class ArrayHolder {
		@Id
		Integer id;

		String[] textArray;

		String[] textArray2;

		Integer[] intArray;

		public ArrayHolder() {
		}

		public ArrayHolder(Integer id, Integer[] intArray, String[] textArray) {
			this.id = id;
			this.intArray = intArray;
			this.textArray = textArray;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Integer[] getIntArray() {
			return intArray;
		}

		public void setIntArray(Integer[] intArray) {
			this.intArray = intArray;
		}

		public String[] getTextArray() {
			return textArray;
		}

		public void setTextArray(String[] textArray) {
			this.textArray = textArray;
		}

		public String[] getTextArray2() {
			return textArray2;
		}

		public void setTextArray2(String[] textArray2) {
			this.textArray2 = textArray2;
		}
	}

}
