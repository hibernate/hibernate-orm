/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.formulajoin;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gavin King
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/formulajoin/Root.hbm.xml"
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = AvailableSettings.JPA_METAMODEL_POPULATION, value = "enabled")
)
public class FormulaJoinTest {

	@Test
	public void testFormulaJoin(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Root root = new Root();
					root.setName( "root 1" );
					Detail current = new Detail();
					current.setCurrentVersion( true );
					current.setVersion( 2 );
					current.setDetails( "details of root 1 blah blah" );
					current.setRoot( root );
					root.setDetail( current );
					Detail past = new Detail();
					past.setCurrentVersion( false );
					past.setVersion( 1 );
					past.setDetails( "old details of root 1 yada yada" );
					past.setRoot( root );
					session.persist( root );
					session.persist( past );
					session.persist( current );
				}
		);

		if ( scope.getSessionFactory().getJdbcServices().getDialect() instanceof PostgreSQLDialect ) {
			return;
		}

		scope.inTransaction(
				session -> {
					List<Object[]> l = session.createQuery( "from Root m left join m.detail d", Object[].class ).list();
					assertThat( l ).hasSize( 1 );
				}
		);

		scope.inTransaction(
				session -> {
					List<Root> l = session.createQuery( "from Root m left join fetch m.detail", Root.class ).list();
					assertThat( l ).hasSize( 1 );
					Root m = l.get( 0 );
					assertThat( m.getDetail().getRoot().getName() ).isEqualTo( "root 1" );
					assertThat( m.getDetail().getRoot() ).isEqualTo( m );
				}
		);

		scope.inTransaction(
				session -> {
					List<Root> l = session.createQuery( "from Root m join fetch m.detail", Root.class ).list();
					assertThat( l ).hasSize( 1 );
				}
		);

		scope.inTransaction(
				session -> {
					List<Detail> l = session.createQuery( "from Detail d join fetch d.currentRoot.root", Detail.class )
							.list();
					assertThat( l ).hasSize( 2 );
				}
		);

		scope.inTransaction(
				session -> {
					List<Detail> l = session.createQuery( "from Detail d join fetch d.root", Detail.class ).list();
					assertThat( l ).hasSize( 2 );
				}
		);

		scope.inTransaction(
				session -> {
					List<Detail> l = session.createQuery( "from Detail d join fetch d.currentRoot.root m join fetch m.detail",
							Detail.class ).list();
					assertThat( l ).hasSize( 2 );
				}
		);

		scope.inTransaction(
				session -> {
					List<Detail> l = session.createQuery( "from Detail d join fetch d.root m join fetch m.detail",
							Detail.class ).list();
					assertThat( l ).hasSize( 2 );

					session.createMutationQuery( "delete from Detail" ).executeUpdate();
					session.createMutationQuery( "delete from Root" ).executeUpdate();
				}
		);
	}
}
