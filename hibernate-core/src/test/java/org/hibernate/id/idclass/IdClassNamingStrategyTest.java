/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.idclass;

import org.hibernate.boot.model.naming.ImplicitNamingStrategyComponentPathImpl;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

public class IdClassNamingStrategyTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { MyEntity.class };
	}

	@Override
	protected void configure(Configuration configuration) {
        /*
         * With this implicit naming strategy, we got the following mapping:
         *
         * create table MyEntity (
         *   id_idA bigint not null,
         *   id_idB bigint not null,
         *   _identifierMapper_idA bigint not null, <-- ??
         *   _identifierMapper_idB bigint not null, <-- ??
         *   notes varchar(255),
         *   primary key (id_idA, id_idB)
         * )
         */
		configuration.setImplicitNamingStrategy( new ImplicitNamingStrategyComponentPathImpl() );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-14241")
	public void test() {
		inTransaction( ( session ) -> {
			MyEntity entity = new MyEntity();
			entity.setId( new MyEntityId( 739L, 777L ) );

			session.persist( entity );
		} );
	}
}
