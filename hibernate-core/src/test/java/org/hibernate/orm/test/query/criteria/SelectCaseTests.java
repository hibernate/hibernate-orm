/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.criteria;

import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.boot.MetadataSources;
import org.hibernate.query.sqm.NodeBuilder;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics_;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("WeakerAccess")
@TestForIssue(jiraKey = "HHH-10843")
public class SelectCaseTests extends SessionFactoryBasedFunctionalTest {
	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		StandardDomainModel.GAMBIT.getDescriptor().applyDomainModel( metadataSources );
	}

	@BeforeAll
	public void createTestData() {
		inTransaction(
				session -> {
					final EntityOfBasics entity = new EntityOfBasics();
					entity.setId( 1 );
					entity.setTheInteger( 1 );
					entity.setTheString( "test_1" );
					session.save( entity );
				}
		);
	}

	@AfterAll
	public void dropTestData() {
		inTransaction(
				session -> {
					session.createQuery( "delete from EntityOfBasics" ).executeUpdate();
				}
		);
	}

	@Test
	public void testSelectCaseWithConcat() {
		inTransaction(
				session -> {
					final NodeBuilder nb = session.getCriteriaBuilder();
					final CriteriaQuery<Object> criteria = nb.createQuery();
					final Root<EntityOfBasics> root = criteria.from( EntityOfBasics.class );
					criteria.select(
							nb.selectCase()
									.when( nb.isNotNull( root.get( EntityOfBasics_.theInteger ) ), nb.concat( "test_", nb.literal( "1" ) ) )
									.otherwise( nb.literal( "Empty" ) )
					);

					assertThat(
							session.createQuery( criteria ).getSingleResult(),
							notNullValue()
					);
				}
		);
	}
}
