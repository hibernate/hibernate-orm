/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.id;

import org.hibernate.FlushMode;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNoColumnInsert.class)
@TestForIssue(jiraKey = "HHH-12464")
@DomainModel(
		annotatedClasses = {
				RootEntity.class,
				RelatedEntity.class,
		}
)
@SessionFactory
public class CreateDeleteTest {

	@Test
	public void createAndDeleteAnEntityInTheSameTransactionTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.setHibernateFlushMode( FlushMode.COMMIT );
					RootEntity entity = new RootEntity();
					session.persist( entity );
					session.delete( entity );
				} );
	}

}
