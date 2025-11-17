/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.quote;

import java.util.Iterator;

import org.hibernate.Transaction;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Emmanuel Bernard
 * @author Brett Meyer
 */
@DomainModel(
		annotatedClasses = {
				User.class,
				Role.class,
				Phone.class,
				Person.class,
				House.class
		},
		xmlMappings = "org/hibernate/orm/test/quote/DataPoint.hbm.xml"
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, value = "true")
)
public class QuoteGlobalTest {

	@Test
	@JiraKey(value = "HHH-7890")
	public void testQuotedUniqueConstraint(SessionFactoryScope scope) {
		Iterator<UniqueKey> itr = scope.getMetadataImplementor().getEntityBinding(Person.class.getName())
				.getTable().getUniqueKeys().values().iterator();
		while ( itr.hasNext() ) {
			UniqueKey uk = itr.next();
			assertEquals( 1, uk.getColumns().size() );
			assertEquals( "name", uk.getColumn( 0 ).getName() );
			return;
		}
		fail( "GLOBALLY_QUOTED_IDENTIFIERS caused the unique key creation to fail." );
	}

	@Test
	public void testQuoteManytoMany(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					try {
						Transaction tx = session.beginTransaction();
						User u = new User();
						session.persist( u );
						Role r = new Role();
						session.persist( r );
						u.getRoles().add( r );
						session.flush();
						session.clear();
						u = session.get( User.class, u.getId() );
						assertEquals( 1, u.getRoles().size() );
						tx.rollback();
						String role = User.class.getName() + ".roles";
						assertEquals(
								"User_Role",
								scope.getMetadataImplementor()
										.getCollectionBinding( role )
										.getCollectionTable()
										.getName()
						);
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-8520")
	public void testHbmQuoting(SessionFactoryScope scope) {
		final MetadataImplementor metadataImplementor = scope.getMetadataImplementor();
		doTestHbmQuoting( DataPoint.class, metadataImplementor );
		doTestHbmQuoting( AssociatedDataPoint.class, metadataImplementor );
	}

	private void doTestHbmQuoting(Class clazz, MetadataImplementor metadataImplementor) {
		Table table = metadataImplementor.getEntityBinding( clazz.getName() ).getTable();
		assertTrue( table.isQuoted() );
		Iterator itr = table.getColumns().iterator();
		while ( itr.hasNext() ) {
			Column column = (Column) itr.next();
			assertTrue( column.isQuoted() );
		}
	}
}
