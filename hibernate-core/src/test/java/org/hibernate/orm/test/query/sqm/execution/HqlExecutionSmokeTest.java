/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm.execution;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.SessionFactoryBasedFunctionalTest;
import org.hibernate.orm.test.support.domains.gambit.EntityOfBasics;

import org.hibernate.testing.junit5.StandardTags;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.orm.test.support.domains.retail.ModelClasses.applyRetailModel;
import static org.hibernate.testing.hamcrest.CollectionMatchers.hasSize;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
@Tag(StandardTags.SQM )
public class HqlExecutionSmokeTest extends SessionFactoryBasedFunctionalTest {
	@BeforeEach
	public void createData() {
// currently a problem with EntityEntry -> PC
		sessionFactoryScope().inTransaction(
				session -> {
					session.doWork(
							connection -> {
								final Statement statement = connection.createStatement();
								try {
									statement.execute(
											"insert into EntityOfBasics( id, gender, theInt ) values ( 1, 'M', -1 )" );
								}
								finally {
									try {
										statement.close();
									}
									catch (SQLException ignore) {
									}
								}
							}
					);
				}
		);
	}

	@AfterEach
	public void dropData() {
		sessionFactoryScope().inTransaction(
				session -> session.doWork(
						connection -> {
							try (Statement statement = connection.createStatement() ) {
								statement.execute( "delete from EntityOfBasics" );
							}
						}
				)
		);
	}

	@Test
	public void testQueryExecution() {
		sessionFactoryScope().inTransaction(
				session -> {
					final List result = session.createQuery( "select e.id from EntityOfBasics e" ).list();
					assertThat( result, hasSize( 1 ) );
					final Object value = result.get( 0 );
					assertThat( value, instanceOf( Integer.class ) );
					assertThat( value, is( 1 ) );
				}
		);
	}

	@Test
	public void testEntityWithSecondaryTable() {
		sessionFactoryScope().inTransaction(
				session -> session.createQuery( "from Vendor" ).list()
		);
	}

	@Override
	protected boolean exportSchema() {
		return true;
	}

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( EntityOfBasics.class );
		applyRetailModel( metadataSources );
	}
}
