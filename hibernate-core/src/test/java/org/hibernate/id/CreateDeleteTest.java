/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import org.hibernate.FlushMode;
import org.hibernate.dialect.AbstractHANADialect;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

@RequiresDialectFeature(DialectChecks.SupportsIdentityColumns.class)
@TestForIssue(jiraKey = "HHH-12464")
public class CreateDeleteTest extends BaseCoreFunctionalTestCase {
	@Test
	@SkipForDialect(value = AbstractHANADialect.class, comment = " HANA doesn't support tables consisting of only a single auto-generated column")
	public void createAndDeleteAnEntityInTheSameTransactionTest() {
		doInHibernate( this::sessionFactory, session -> {
			session.setHibernateFlushMode( FlushMode.COMMIT );
			RootEntity entity = new RootEntity();
			session.persist( entity );
			session.delete( entity );
		} );
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				RootEntity.class,
				RelatedEntity.class,
		};
	}
}
