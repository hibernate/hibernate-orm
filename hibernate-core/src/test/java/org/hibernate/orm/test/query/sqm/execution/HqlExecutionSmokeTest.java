/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm.execution;

import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.LobHelper;
import org.hibernate.boot.MetadataSources;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.orm.test.SessionFactoryBasedFunctionalTest;
import org.hibernate.orm.test.support.domains.gambit.EntityOfBasics;
import org.hibernate.type.descriptor.java.internal.LobStreamDataHelper;

import org.hibernate.testing.junit5.StandardTags;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.hibernate.orm.test.support.util.LobHelper.createClob;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
@TestInstance( TestInstance.Lifecycle.PER_CLASS )
@Tag(StandardTags.SQM )
public class HqlExecutionSmokeTest extends SessionFactoryBasedFunctionalTest {
	@BeforeEach
	public void createData() {
// currently a with EntityEntry -> PC
//		sessionFactoryScope().inTransaction(
//				session -> {
//					session.doWork(
//							connection -> {
//								final Statement statement = connection.createStatement();
//								try {
//									statement.execute( "insert into EntityOfBasics( id, gender, theInt ) values ( 1, 'M', -1 )" );
//								}
//								finally {
//									try {
//										statement.close();
//									}
//									catch (SQLException ignore) {
//									}
//								}
//							}
//					);
//
//
// currently a problem saving entities
//					EntityOfBasics entity = new EntityOfBasics();
//					entity.setGender( EntityOfBasics.Gender.FEMALE );
//					entity.setTheInt( 5 );
//					entity.setTheInteger( null );
//					entity.setTheString( StringHelper.repeat( 'x', 250 ) );
//					entity.setTheUrl( null );
//					entity.setTheClob( createClob( 'c', 5000 ) );
//
//					session.save( entity );
//				}
//		);
	}

	@Test
	public void testQueryExecution() {
		sessionFactoryScope().inTransaction(
				session -> session.createQuery( "select e from EntityOfBasics e" ).list()
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
	}
}
