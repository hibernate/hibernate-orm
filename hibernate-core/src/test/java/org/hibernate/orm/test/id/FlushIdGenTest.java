/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.orm.test.id;

import org.hibernate.Transaction;
import org.hibernate.dialect.AbstractHANADialect;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DialectFeatureChecks.SupportsIdentityColumns;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

@TestForIssue(jiraKey = "HHH-8611")
@RequiresDialectFeature(feature = SupportsIdentityColumns.class)
@DomainModel(
		annotatedClasses = {
				RootEntity.class,
				RelatedEntity.class,
		}
)
@SessionFactory
public class FlushIdGenTest {

	@Test
	@SkipForDialect(dialectClass = AbstractHANADialect.class, matchSubTypes = true, reason = " HANA doesn't support tables consisting of only a single auto-generated column")
	public void testPersistBeforeTransaction(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					RootEntity ent1_0 = new RootEntity();
					RootEntity ent1_1 = new RootEntity();

					session.persist( ent1_0 );
					session.persist( ent1_1 );

					Transaction tx = session.beginTransaction();
					try {
						tx.commit(); // flush
					}
					catch (Exception e) {
						tx.rollback();
					}
				}
		);

	}
}
