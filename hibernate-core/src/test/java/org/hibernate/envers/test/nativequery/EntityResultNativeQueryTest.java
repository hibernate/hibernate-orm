/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.nativequery;

import java.util.List;

import javax.persistence.Query;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.nativequery.SecondSimpleEntity;
import org.hibernate.envers.test.support.domains.nativequery.SimpleEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Andrea Boriero
 */
@Disabled("NYI - Native Query Support")
public class EntityResultNativeQueryTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { SimpleEntity.class, SecondSimpleEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransaction( entityManager -> { entityManager.persist( new SimpleEntity( "Hibernate" ) ); } );
	}

	@DynamicTest
	@TestForIssue(jiraKey = "HHH-12776")
	public void testNativeQueryResultHandling() {
		inTransaction(
				entityManager -> {
					Query query = entityManager.createNativeQuery( "select * from SimpleEntity", SimpleEntity.class );
					List results = query.getResultList();
					SimpleEntity result = (SimpleEntity) results.get( 0 );
					assertThat( result.getStringField(), is( "Hibernate" ) );
				}
		);
	}

}

