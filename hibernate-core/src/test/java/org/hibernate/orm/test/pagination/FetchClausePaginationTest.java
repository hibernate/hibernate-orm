/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pagination;

import java.util.List;

import org.hibernate.community.dialect.DerbyDialect;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.EntityOfLists;
import org.hibernate.testing.orm.domain.gambit.EnumValue;
import org.hibernate.testing.orm.domain.gambit.SimpleComponent;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 *
 * @author Christian Beikov
 */
@DomainModel( standardModels = StandardDomainModel.GAMBIT )
@ServiceRegistry
@SessionFactory
public class FetchClausePaginationTest {
	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityOfLists entityContainingLists = new EntityOfLists( 1, "first" );

					entityContainingLists.addBasic( "abc" );
					entityContainingLists.addBasic( "def" );
					entityContainingLists.addBasic( "ghi" );

					entityContainingLists.addConvertedEnum( EnumValue.TWO );

					entityContainingLists.addEnum( EnumValue.ONE );
					entityContainingLists.addEnum( EnumValue.THREE );

					entityContainingLists.addComponent( new SimpleComponent( "first-a1", "first-another-a1" ) );
					entityContainingLists.addComponent( new SimpleComponent( "first-a2", "first-another-a2" ) );
					entityContainingLists.addComponent( new SimpleComponent( "first-a3", "first-another-a2" ) );

					session.persist( entityContainingLists );
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsWithTies.class)
	@SkipForDialect(dialectClass = DerbyDialect.class, reason = "Derby only supports row_number, but this requires the dense_rank window function")
	public void testFetchWithTies(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					List<SimpleComponent> list = session.createQuery(
							"select comp from EntityOfLists e join e.listOfComponents comp order by comp.anotherAttribute desc fetch first 1 row with ties",
							SimpleComponent.class
					).list();
					assertThat( list.size(), is( 2 ) );
				}
		);
	}
}
