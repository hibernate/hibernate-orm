/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.dirty;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;
import org.hibernate.test.bytecode.enhancement.EnhancerTestUtils;

/**
 * @author Luis Barreiro
 */
public class DirtyTrackingTestTask extends AbstractEnhancerTestTask {

	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {SimpleEntity.class};
	}

	public void prepare() {
	}

	public void execute() {
		SimpleEntity entity = new SimpleEntity();

		// Basic single field
		entity.getId();
		EnhancerTestUtils.checkDirtyTracking( entity );
		entity.setId( 1l );
		EnhancerTestUtils.checkDirtyTracking( entity, "id" );
		EnhancerTestUtils.clearDirtyTracking( entity );
		entity.setId( entity.getId() );
		EnhancerTestUtils.checkDirtyTracking( entity );

		// Basic multi-field
		entity.setId( 2l );
		entity.setActive( !entity.isActive() );
		entity.setSomeNumber( 193L );
		EnhancerTestUtils.checkDirtyTracking( entity, "id", "active", "someNumber" );
		EnhancerTestUtils.clearDirtyTracking( entity );

		// Setting the same value should not make it dirty
		entity.setSomeNumber( 193L );
		EnhancerTestUtils.checkDirtyTracking( entity );

		// Collection
		List<String> strings = new ArrayList<String>();
		strings.add( "FooBar" );
		entity.setSomeStrings( strings );
		EnhancerTestUtils.checkDirtyTracking( entity, "someStrings" );
		EnhancerTestUtils.clearDirtyTracking( entity );

		strings.add( "BarFoo" );
		EnhancerTestUtils.checkDirtyTracking( entity, "someStrings" );
		EnhancerTestUtils.clearDirtyTracking( entity );

		// Association: this should not set the entity to dirty
		Set<Integer> intSet = new HashSet<Integer>();
		intSet.add( 42 );
		entity.setSomeInts( intSet );
		EnhancerTestUtils.checkDirtyTracking( entity );

		// testing composite object
		Address address = new Address();
		entity.setAddress( address );
		address.setCity( "Arendal" );
		EnhancerTestUtils.checkDirtyTracking( entity, "address", "address.city" );
		EnhancerTestUtils.clearDirtyTracking( entity );

		// make sure that new composite instances are cleared
		Address address2 = new Address();
		entity.setAddress( address2 );
		address.setStreet1( "Heggedalveien" );
		EnhancerTestUtils.checkDirtyTracking( entity, "address" );

		Country country = new Country();
		address2.setCountry( country );
		country.setName( "Norway" );
		EnhancerTestUtils.checkDirtyTracking( entity, "address", "address.country", "address.country.name" );

	}

	protected void cleanup() {
	}
}
