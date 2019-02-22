/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.tools;

import org.hibernate.envers.test.EnversSessionFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-7106")
public class SchemaExportTest extends EnversSessionFactoryBasedFunctionalTest {
	private Integer id = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { StrTestEntity.class };
	}

	@DynamicTest
	public void testSchemaCreationAndRetrieval() {
		this.id = inTransaction(
				session -> {
					final StrTestEntity entity = new StrTestEntity( "data" );
					session.save( entity );
					return entity.getId();
				}
		);

		assertThat( getAuditReader().getRevisions( StrTestEntity.class, id ), contains( 1 ) );
		assertThat( getAuditReader().find( StrTestEntity.class, id, 1 ), equalTo( new StrTestEntity( id, "data" ) ) );
	}
}
