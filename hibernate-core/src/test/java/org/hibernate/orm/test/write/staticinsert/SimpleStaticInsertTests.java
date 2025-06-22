/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.write.staticinsert;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.retail.CardPayment;
import org.hibernate.testing.orm.domain.retail.DomesticVendor;
import org.hibernate.testing.orm.domain.retail.ForeignVendor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple tests for new "write path" approach in cases of static (non-dynamic) inserts
 *
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public class SimpleStaticInsertTests {

	@Test
	@DomainAndFactory
	public void simpleSingleTableWithSecondaryTableTest(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			session.persist( new DomesticVendor( 1, "Acme Anvil Inc.", "Acme Worldwide") );
			session.persist( new ForeignVendor( 2, "Acme Train Inc.", "Acme Worldwide") );
		} );

		// supplemental details are null, so we should have no inserts for the secondary table
		assertThat( statementInspector.getSqlQueries() ).hasSize( 2 );
	}

	@Test
	@DomainAndFactory
	public void simpleSingleTableWithSecondaryTableTest2(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			session.persist( new DomesticVendor( 1, "Acme Anvil Inc.", "Acme Worldwide", "supplemental details - domestic" ) );
			session.persist( new ForeignVendor( 2, "Acme Train Inc.", "Acme Worldwide", "supplemental details - foreign" ) );
		} );

		// supplemental details are non-null, so we should have inserts for the secondary table
		assertThat( statementInspector.getSqlQueries() ).hasSize( 4 );
	}

	@Test
	@DomainAndFactory
	public void simpleJoinedTest(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			session.persist( new CardPayment( 1, 123456, 1L, "USD" ) );
			session.persist( new CardPayment( 2, 456789, 200L, "USD" ) );
		} );

		assertThat( statementInspector.getSqlQueries() ).hasSize( 4 );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Target({ ElementType.TYPE, ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	@DomainModel( standardModels = StandardDomainModel.RETAIL )
	@SessionFactory( useCollectingStatementInspector = true )
	@interface DomainAndFactory {}
}
