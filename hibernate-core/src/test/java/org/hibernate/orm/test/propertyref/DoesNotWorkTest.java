/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.propertyref;

import java.util.List;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@RequiresDialect(H2Dialect.class)
@DomainModel(
		annotatedClasses = DoesNotWork.class
)
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false"),
				@Setting(name = AvailableSettings.HBM2DDL_IMPORT_FILES, value = "/org/hibernate/orm/test/propertyref/import.sql")
		}
)
public class DoesNotWorkTest {

	@Test
	public void testIt(SessionFactoryScope scope) {
		DoesNotWorkPk pk = new DoesNotWorkPk();
		pk.setId1( "ZZZ" );
		pk.setId2( "00" );

//		{
//			Session session = openSession();
//			session.beginTransaction();
//			DoesNotWork entity = new DoesNotWork( pk );
//			entity.setGlobalNotes( Arrays.asList( "My first note!" ) );
//			session.persist( entity );
//			session.getTransaction().commit();
//			session.close();
//		}

		scope.inTransaction(
				session -> {
					DoesNotWork entity = (DoesNotWork) session.get( DoesNotWork.class, pk );
					List<String> notes = entity.getGlobalNotes();
					if ( notes != null && notes.size() > 0 ) {
						for ( String s : notes ) {
							System.out.println( s );
						}
					}
					session.remove( entity );
				}
		);
	}
}
