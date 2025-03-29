/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.fetch.subselect;

import java.util.List;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.mapping.Collection;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RequiresDialect(SQLServerDialect.class)
@RequiresDialect(SybaseDialect.class)
public class SubselectFetchWithFormulaTransactSqlTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected String getBaseForMappings() {
		return "";
	}

	@Override
	protected String[] getMappings() {
		return new String[] {
				"mappings/subselectfetch/NameTransactSql.hbm.xml",
				"mappings/subselectfetch/Value.hbm.xml"
		};
	}

	@Before
	public void before() {
		inTransaction(
				session -> {
					Name chris = new Name();
					chris.setId( 1 );
					chris.setName( "chris" );
					Value cat = new Value();
					cat.setId(1);
					cat.setName( chris );
					cat.setValue( "cat" );
					Value canary = new Value();
					canary.setId( 2 );
					canary.setName( chris );
					canary.setValue( "canary" );

					session.persist( chris );
					session.persist( cat );
					session.persist( canary );

					Name sam = new Name();
					sam.setId(2);
					sam.setName( "sam" );
					Value seal = new Value();
					seal.setId( 3 );
					seal.setName( sam );
					seal.setValue( "seal" );
					Value snake = new Value();
					snake.setId( 4 );
					snake.setName( sam );
					snake.setValue( "snake" );

					session.persist( sam );
					session.persist(seal);
					session.persist( snake );
				}
		);
	}

	@After
	public void after() {
		inTransaction(
				session -> {
					session.createQuery( "delete Value" ).executeUpdate();
					session.createQuery( "delete Name" ).executeUpdate();
				}
		);
	}

	@Test
	public void checkSubselectWithFormula() throws Exception {
		// as a pre-condition make sure that subselect fetching is enabled for the collection...
		Collection collectionBinding = metadata().getCollectionBinding( Name.class.getName() + ".values" );
		assertThat( collectionBinding.isSubselectLoadable(), is( true ) );

		// Now force the subselect fetch and make sure we do not get SQL errors
		inTransaction(
				session -> {
					CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
					CriteriaQuery<Name> criteria = criteriaBuilder.createQuery( Name.class );
					criteria.from( Name.class );
					List<Name> results = session.createQuery( criteria ).list();
//					List results = session.createCriteria(Name.class).list();
					for (Name name : results) {
						name.getValues().size();
					}
				}
		);
	}

}
