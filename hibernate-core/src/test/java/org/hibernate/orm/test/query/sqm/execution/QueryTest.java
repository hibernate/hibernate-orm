/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm.execution;

import org.hibernate.boot.MetadataSources;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.junit.jupiter.api.Test;

/**
 * @author Gavin King
 */
public class QueryTest extends SessionFactoryBasedFunctionalTest {

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		StandardDomainModel.GAMBIT.getDescriptor().applyDomainModel( metadataSources );
	}

	@Test
	public void testComment() {
		inTransaction(
				session -> {
					session.createQuery("/*select everything*/ from /*the entity*/ EntityOfBasics").list();
				}
		);
	}

	@Test
	public void testSelectAfterFrom() {
		inTransaction(
				session -> {
					session.createQuery("from EntityOfBasics e select e.id, e.theString").list();
					session.createQuery("from EntityOfBasics e where e.theString is not null select e.id, e.theString order by e.theString").list();
				}
		);
	}

	@Test
	public void testNoFrom() {
		inTransaction(
				session -> {
					session.createQuery("select 'hello'").list();
					session.createQuery("select current_timestamp, current_date").list();
				}
		);
	}

}
