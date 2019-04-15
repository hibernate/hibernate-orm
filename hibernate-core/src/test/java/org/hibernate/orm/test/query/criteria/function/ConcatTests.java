/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.criteria.function;

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

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("WeakerAccess")
@TestForIssue(jiraKey = "HHH-10843")
public class ConcatTests extends SessionFactoryBasedFunctionalTest {
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
					entity.setTheInt( 1 );
					entity.setTheString( "some string" );
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
	public void testConcat() {
		inTransaction(
				session -> {
					final NodeBuilder nb = session.getCriteriaBuilder();
					final CriteriaQuery<Integer> criteria = nb.createQuery( Integer.class );
					final Root<EntityOfBasics> root = criteria.from( EntityOfBasics.class );
					criteria.select( root.get( EntityOfBasics_.id ) );
					criteria.where(
							nb.equal(
									root.get( EntityOfBasics_.theString ),
									nb.concat(
											nb.literal( "some" ),
											nb.concat(
													nb.literal( " " ),
													"string"
											)
									)
							)
					);
				}
		);
	}
}
