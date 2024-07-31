/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.envers.integration.nativequery;

import java.util.List;
import jakarta.persistence.Query;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
public class EntityResultNativeQueryTest extends BaseEnversJPAFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { SimpleEntity.class, SecondSimpleEntity.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.persist( new SimpleEntity( "Hibernate" ) );
		} );
	}

	@Test
	@JiraKey(value = "HHH-12776")
	public void testNativeQueryResultHandling() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Query query = entityManager.createNativeQuery( "select * from SimpleEntity", SimpleEntity.class );
			List results = query.getResultList();
			SimpleEntity result = (SimpleEntity) results.get( 0 );
			assertThat( result.getStringField(), is( "Hibernate" ) );
		} );
	}

}

