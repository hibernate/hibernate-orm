/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.extended;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.engine.spi.PersistentAttributeInterceptable;

import org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils;
import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;
import org.hibernate.test.bytecode.enhancement.basic.ObjectAttributeMarkerInterceptor;
import org.junit.Assert;

/**
 * @author Luis Barreiro
 */
public class ExtendedEnhancementTestTask extends AbstractEnhancerTestTask {

	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {SimpleEntity.class};
	}

	public void prepare() {
	}

	public void execute() {
		// test uses ObjectAttributeMarkerInterceptor to ensure that field access is routed through enhanced methods

		SimpleEntity entity = new SimpleEntity();
		( (PersistentAttributeInterceptable) entity ).$$_hibernate_setInterceptor( new ObjectAttributeMarkerInterceptor() );

		Object decoy = new Object();
		entity.anUnspecifiedObject = decoy;

		Object gotByReflection = EnhancerTestUtils.getFieldByReflection( entity, "anUnspecifiedObject" );
		Assert.assertNotSame( gotByReflection, decoy );
		Assert.assertSame( gotByReflection, ObjectAttributeMarkerInterceptor.WRITE_MARKER );

		Object entityObject = entity.anUnspecifiedObject;

		Assert.assertNotSame( entityObject, decoy );
		Assert.assertSame( entityObject, ObjectAttributeMarkerInterceptor.READ_MARKER );

		// do some more calls on the various types, without the interceptor
		( (PersistentAttributeInterceptable) entity ).$$_hibernate_setInterceptor( null );

		entity.id = 1234567890l;
		Assert.assertEquals( entity.id, 1234567890l );

		entity.name = "Entity Name";
		Assert.assertSame( entity.name, "Entity Name" );

		entity.active = true;
		Assert.assertTrue( entity.active );

		entity.someStrings = Arrays.asList( "A", "B", "C", "D" );
		Assert.assertArrayEquals( new String[] { "A", "B", "C", "D" }, entity.someStrings.toArray() );
	}

	protected void cleanup() {
	}

	@Entity public class SimpleEntity {

		@Id public long id;

		public String name;

		public boolean active;

		public long someNumber;

		public int anInt;

		public Object anUnspecifiedObject;

		public List<String> someStrings;

		@OneToMany public Set<Integer> someInts;
	}
}
