/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.fetch.subselect;

import org.hibernate.community.dialect.FirebirdDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.mapping.Collection;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@SuppressWarnings("JUnitMalformedDeclaration")
@SkipForDialect(dialectClass = SQLServerDialect.class)
@SkipForDialect(dialectClass = SybaseDialect.class)
@SkipForDialect(dialectClass = FirebirdDialect.class)
@DomainModel(xmlMappings = {"mappings/subselectfetch/name.xml", "mappings/subselectfetch/value.xml"})
@SessionFactory(useCollectingStatementInspector = true)
@FailureExpected(reason = "https://hibernate.atlassian.net/browse/HHH-19316")
public class SubselectFetchWithFormulaTest {
	static void prepareTestData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final Name chris = new Name();
			chris.setId( 1 );
			chris.setName( "chris" );

			final Value cat = new Value();
			cat.setId(1);
			cat.setName( chris );
			cat.setValue( "cat" );

			final Value canary = new Value();
			canary.setId( 2 );
			canary.setName( chris );
			canary.setValue( "canary" );

			session.persist( chris );
			session.persist( cat );
			session.persist( canary );

			final Name sam = new Name();
			sam.setId(2);
			sam.setName( "sam" );

			final Value seal = new Value();
			seal.setId( 3 );
			seal.setName( sam );
			seal.setValue( "seal" );

			final Value snake = new Value();
			snake.setId( 4 );
			snake.setName( sam );
			snake.setValue( "snake" );

			session.persist( sam );
			session.persist(seal);
			session.persist( snake );
		} );
	}

	@BeforeEach
	void createTestData(SessionFactoryScope factoryScope) {
		prepareTestData( factoryScope );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void checkSubselectWithFormula(DomainModelScope modelScope, SessionFactoryScope factoryScope) {
		verify( modelScope, factoryScope );

	}

	static void verify(DomainModelScope modelScope, SessionFactoryScope factoryScope) {
		// as a pre-condition make sure that subselect fetching is enabled for the collection...
		Collection collectionBinding = modelScope.getDomainModel().getCollectionBinding( Name.class.getName() + ".values" );
		assertThat( collectionBinding.isSubselectLoadable() ).isTrue();


		// Now force the subselect fetch and make sure we do not get SQL errors
		factoryScope.inTransaction( (session) -> {
			final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();
			final List<Name> names = session.createSelectionQuery( "from Name", Name.class ).list();
			sqlCollector.clear();

			names.forEach( (name) -> {
				assertThat( name.getValues() ).hasSize( 2 );
			} );
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );

		} );
	}

}
