/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.components.relations;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;
import org.hibernate.envers.test.support.domains.components.relations.OneToManyComponent;
import org.hibernate.envers.test.support.domains.components.relations.OneToManyComponentTestEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class OneToManyInComponentTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer otmcte_id1;
	private Integer ste_id1;
	private Integer ste_id2;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { OneToManyComponentTestEntity.class, StrTestEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		final StrTestEntity ste1 = new StrTestEntity();
		ste1.setStr( "str1" );

		final StrTestEntity ste2 = new StrTestEntity();
		ste2.setStr( "str2" );

		inTransactions(
				// Revision 1
				entityManager -> {
					entityManager.persist( ste1 );
					entityManager.persist( ste2 );

					this.ste_id1 = ste1.getId();
					this.ste_id2 = ste2.getId();
				},

				// Revision 2
				entityManager -> {
					OneToManyComponentTestEntity otmcte1 = new OneToManyComponentTestEntity( new OneToManyComponent( "data1" ) );
					otmcte1.getComp1().getEntities().add( ste1 );

					entityManager.persist( otmcte1 );

					this.otmcte_id1 = otmcte1.getId();
				},

				// Revision 3
				entityManager -> {
					OneToManyComponentTestEntity otmcte1 = entityManager.find( OneToManyComponentTestEntity.class, otmcte_id1 );
					otmcte1.getComp1().getEntities().add( ste2 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( OneToManyComponentTestEntity.class, otmcte_id1 ), contains( 2, 3 ) );
	}

	@DynamicTest
	public void testHistoryOfId1() {
		final OneToManyComponentTestEntity ver2 = inTransaction(
				entityManager -> {
					final StrTestEntity ste1 = entityManager.find( StrTestEntity.class, ste_id1 );
					final OneToManyComponent component = new OneToManyComponent( "data1" );

					final OneToManyComponentTestEntity entity = new OneToManyComponentTestEntity( otmcte_id1, component );
					entity.getComp1().getEntities().add( ste1 );

					return entity;
				}
		);

		final OneToManyComponentTestEntity ver3 = inTransaction(
				entityManager -> {
					final StrTestEntity ste1 = entityManager.find( StrTestEntity.class, ste_id1 );
					final StrTestEntity ste2 = entityManager.find( StrTestEntity.class, ste_id2 );
					final OneToManyComponent component = new OneToManyComponent( "data1" );

					final OneToManyComponentTestEntity entity = new OneToManyComponentTestEntity( otmcte_id1, component );
					entity.getComp1().getEntities().add( ste1 );
					entity.getComp1().getEntities().add( ste2 );

					return entity;
				}
		);

		assertThat( getAuditReader().find( OneToManyComponentTestEntity.class, otmcte_id1, 1 ), nullValue() );
		assertThat( getAuditReader().find( OneToManyComponentTestEntity.class, otmcte_id1, 2 ), equalTo( ver2 ) );
		assertThat( getAuditReader().find( OneToManyComponentTestEntity.class, otmcte_id1, 3 ), equalTo( ver3 ) );
	}
}