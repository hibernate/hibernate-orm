/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stateless;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.HANADialect;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author stliu
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/stateless/Contact.hbm.xml"
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = Environment.MAX_FETCH_DEPTH, value = "1")
)
public class StatelessSessionQueryTest {

	@Test
	@SkipForDialect(dialectClass = HANADialect.class, matchSubTypes = true, reason = " HANA doesn't support tables consisting of only a single auto-generated column")
	public void testHQL(SessionFactoryScope scope) {
		scope.inStatelessSession(
				session ->
						assertEquals( 1, session.createQuery(
								"from Contact c join fetch c.org o join fetch c.org.country" )
								.list().size() )

		);
	}

	@Test
	@JiraKey(value = "HHH-13194")
	@SkipForDialect(dialectClass = HANADialect.class, matchSubTypes = true, reason = " HANA doesn't support tables consisting of only a single auto-generated column")
	public void testNewQueryApis(SessionFactoryScope scope) {

		final String queryString = "from Contact c join fetch c.org o join fetch o.country";

		scope.inStatelessSession(
				session -> {
					Query query = session.createQuery( queryString );
					assertEquals( 1, query.getResultList().size() );

					query = session.getNamedQuery( Contact.class.getName() + ".contacts" );
					assertEquals( 1, query.getResultList().size() );

					NativeQuery sqlQuery = session.createNativeQuery( "select id from Contact" );
					assertEquals( 1, sqlQuery.getResultList().size() );
				}
		);
	}

	private List list;

	@BeforeEach
	public void createData(SessionFactoryScope scope) {
		list = new ArrayList();
		scope.inTransaction(
				session -> {
					Country usa = new Country();
					session.persist( usa );
					list.add( usa );
					Org disney = new Org();
					disney.setCountry( usa );
					session.persist( disney );
					list.add( disney );
					Contact waltDisney = new Contact();
					waltDisney.setOrg( disney );
					session.persist( waltDisney );
					list.add( waltDisney );
				}
		);
	}

	@AfterEach
	public void cleanData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					for ( Object obj : list ) {
						session.remove( obj );
					}
				}
		);
	}
}
