/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.indexcoll.eager;

import org.hibernate.Hibernate;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.orm.test.annotations.indexcoll.Gas;
import org.hibernate.orm.test.annotations.indexcoll.GasKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Iterator;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Test index collections
 *
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = {
				Atmosphere.class,
				Gas.class,
				GasKey.class
		}
)
@SessionFactory
public class EagerIndexedCollectionTest {

	@AfterEach
	public void afterEach(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testJPA2DefaultMapColumns(SessionFactoryScope scope) {
		isDefaultKeyColumnPresent( Atmosphere.class.getName(), "gasesDef", "_KEY", scope );
		isDefaultKeyColumnPresent( Atmosphere.class.getName(), "gasesPerKeyDef", "_KEY", scope );
		isDefaultKeyColumnPresent( Atmosphere.class.getName(), "gasesDefLeg", "_KEY", scope );
	}

	private void isDefaultKeyColumnPresent(String collectionOwner, String propertyName, String suffix, SessionFactoryScope scope) {
		assertThat( isDefaultColumnPresent( collectionOwner, propertyName, suffix, scope ) )
				.describedAs( "Could not find " + propertyName + suffix )
				.isTrue();
	}

	private boolean isDefaultColumnPresent(String collectionOwner, String propertyName, String suffix, SessionFactoryScope scope) {
		final Collection collection = scope.getMetadataImplementor()
				.getCollectionBinding( collectionOwner + "." + propertyName );
		final Iterator<Column> columnIterator = collection.getCollectionTable().getColumns().iterator();
		boolean hasDefault = false;
		while ( columnIterator.hasNext() ) {
			Column column = columnIterator.next();
			if ( (propertyName + suffix).equals( column.getName() ) ) {
				hasDefault = true;
			}
		}
		return hasDefault;
	}

	@Test
	public void testRealMap(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Atmosphere atm = new Atmosphere();
					Atmosphere atm2 = new Atmosphere();
					GasKey key = new GasKey();
					key.setName( "O2" );
					Gas o2 = new Gas();
					o2.name = "oxygen";
					atm.gases.put( "100%", o2 );
					atm.gasesPerKey.put( key, o2 );
					atm2.gases.put( "100%", o2 );
					atm2.gasesPerKey.put( key, o2 );
					session.persist( key );
					session.persist( atm );
					session.persist( atm2 );

					session.flush();
					session.clear();

					atm = session.find( Atmosphere.class, atm.id );
					key = session.find( GasKey.class, key.getName() );
					assertThat( atm.gases.size() ).isEqualTo( 1 );
					assertThat( atm.gases.get( "100%" ).name ).isEqualTo( o2.name );
					assertThat( atm.gasesPerKey.get( key ).name ).isEqualTo( o2.name );
				}
		);
	}

	@Test
	public void testTemporalKeyMap(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Atmosphere atm = new Atmosphere();
					atm.colorPerDate.put( new Date( 1234567000 ), "red" );
					session.persist( atm );

					session.flush();
					session.clear();

					atm = session.find( Atmosphere.class, atm.id );
					assertThat( atm.colorPerDate.size() ).isEqualTo( 1 );
					final Date date = atm.colorPerDate.keySet().iterator().next();
					final long diff = new Date( 1234567000 ).getTime() - date.getTime();
					assertThat( diff >= 0 && diff < 24 * 60 * 60 * 1000 )
							.describedAs( "24h diff max" )
							.isTrue();
				}
		);
	}

	@Test
	public void testEnumKeyType(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Atmosphere atm = new Atmosphere();
					atm.colorPerLevel.put( Atmosphere.Level.HIGH, "red" );
					session.persist( atm );

					session.flush();
					session.clear();

					atm = session.find( Atmosphere.class, atm.id );
					assertThat( atm.colorPerLevel.size() ).isEqualTo( 1 );
					assertThat( atm.colorPerLevel.get( Atmosphere.Level.HIGH ) ).isEqualTo( "red" );
				}
		);
	}

	@Test
	public void testEntityKeyElementTarget(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Atmosphere atm = new Atmosphere();
					Gas o2 = new Gas();
					o2.name = "oxygen";
					atm.composition.put( o2, 94.3 );
					session.persist( o2 );
					session.persist( atm );

					session.flush();
					session.clear();

					atm = session.find( Atmosphere.class, atm.id );
					assertThat( Hibernate.isInitialized( atm.composition ) ).isTrue();
					assertThat( atm.composition.size() ).isEqualTo( 1 );
					assertThat( atm.composition.keySet().iterator().next().name ).isEqualTo( o2.name );
				}
		);
	}
}
