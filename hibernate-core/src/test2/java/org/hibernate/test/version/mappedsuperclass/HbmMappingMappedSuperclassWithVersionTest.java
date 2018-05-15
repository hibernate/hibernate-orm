/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.version.mappedsuperclass;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-11549")
public class HbmMappingMappedSuperclassWithVersionTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] {"org/hibernate/test/version/mappedsuperclass/TestEntity.hbm.xml"};
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {AbstractEntity.class};
	}

	@Test
	public void testMetamodelContainsHbmVersion() {
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			final TestEntity entity = new TestEntity();
			entity.setName( "Chris" );
			entityManager.persist( entity );
		} );

		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			final CriteriaQuery<TestEntity> query = builder.createQuery( TestEntity.class );
			final Root<TestEntity> root = query.from( TestEntity.class );

			assertThat( root.get( "version" ), is( notNullValue() ) );
		} );
	}

}
