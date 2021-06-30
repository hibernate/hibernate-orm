/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.hql;

import javax.persistence.Tuple;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.contacts.Contact;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Christian Beikov
 */
@ServiceRegistry
@DomainModel(standardModels = StandardDomainModel.CONTACTS)
@SessionFactory
public class GroupByTest {

	@BeforeAll
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction(
				em -> {
					Contact entity1 = new Contact();
					entity1.setId( 123 );
					em.persist( entity1 );
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-1615")
	public void testGroupByEntity(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "select e, count(*) from Contact e group by e", Tuple.class ).list();
				}
		);
	}

}