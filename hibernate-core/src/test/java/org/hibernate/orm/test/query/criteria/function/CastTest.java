/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.criteria.function;

import org.hibernate.boot.MetadataSources;
import org.hibernate.dialect.DerbyDialect;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics_;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class CastTest extends SessionFactoryBasedFunctionalTest {
	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		StandardDomainModel.GAMBIT.getDescriptor().applyDomainModel( metadataSources );
	}

	@AfterEach
	public void cleanUpData() {
		inTransaction(
				session -> {
					// some tests create EntityOfBasics data
					session.createQuery( "delete from EntityOfBasics" ).executeUpdate();
				}
		);
	}

	@Test
	@SkipForDialect(value = DerbyDialect.class,comment = "Derby does not support cast from INTEGER to VARCHAR")
	@TestForIssue( jiraKey = "HHH-5755" )
	public void testCastToString() {
		inTransaction(
				session -> {
					final EntityOfBasics entity = new EntityOfBasics();
					entity.setId( 1 );
					entity.setTheInt( 1 );
					session.save( entity );
				}
		);

		inTransaction(
				session -> {
					final NodeBuilder criteriaBuilder = session.getCriteriaBuilder();
					final SqmSelectStatement<EntityOfBasics> criteria = criteriaBuilder.createQuery( EntityOfBasics.class );
					final SqmRoot<EntityOfBasics> root = criteria.from( EntityOfBasics.class );
					criteria.select( root );
					criteria.where(
							criteriaBuilder.equal(
									root.get( EntityOfBasics_.theInt ).as( String.class ),
									criteriaBuilder.literal( "1" )
							)
					);

					assertThat(
							session.createQuery( criteria ).getSingleResult(),
							notNullValue()
					);
				}
		);
	}
}
