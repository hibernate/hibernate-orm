/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Test;

import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.orm.test.serialization.entity.AnEntity;
import org.hibernate.orm.test.serialization.entity.PK;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.GetterFieldImpl;
import org.hibernate.property.access.spi.GetterMethodImpl;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.property.access.spi.SetterFieldImpl;
import org.hibernate.property.access.spi.SetterMethodImpl;

import org.hibernate.testing.orm.junit.JiraKey;

import static org.junit.Assert.assertSame;

/**
 * Tests that the can access inaccessible private fields and
 * inaccessible protected methods via Getter/Setter.
 *
 * @author Gail Badner
 */
public class GetterSetterSerializationTest {

	@Test
	@JiraKey( value = "HHH-11202")
	public void testPrivateFieldGetter() throws Exception {
		final AnEntity entity = new AnEntity( new PK( 1L ) );

		final String propertyName = "pk";
		final Getter getter = new GetterFieldImpl(
				AnEntity.class,
				propertyName,
				ReflectHelper.findField( AnEntity.class, propertyName)
		);
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject( getter );

		final ObjectInputStream ois = new ObjectInputStream( new ByteArrayInputStream( baos.toByteArray() ) );

		final Getter getterClone = (Getter) ois.readObject();

		assertSame( getter.get( entity ), getterClone.get( entity ) );
	}

	@Test
	@JiraKey( value = "HHH-11202")
	public void testPrivateFieldSetter() throws Exception {
		AnEntity entity = new AnEntity( new PK( 1L ) );

		final String propertyName = "pk";
		final Getter getter = new GetterFieldImpl(
				AnEntity.class,
				propertyName,
				ReflectHelper.findField( AnEntity.class, propertyName)
		);
		final Setter setter = new SetterFieldImpl(
				AnEntity.class,
				propertyName,
				ReflectHelper.findField( AnEntity.class, propertyName)
		);

		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject( setter );

		final ObjectInputStream ois = new ObjectInputStream( new ByteArrayInputStream( baos.toByteArray() ) );

		final Setter setterClone = (Setter) ois.readObject();
		final PK pkNew = new PK( 2L );
		setterClone.set( entity, pkNew  );

		assertSame( pkNew, getter.get( entity ) );
	}

	@Test
	@JiraKey( value = "HHH-11202")
	public void testProtectedMethodGetter() throws Exception {
		final AnEntity entity = new AnEntity( new PK( 1L ) );

		final Getter getter = new GetterMethodImpl(
				AnEntity.class,
				"pk",
				ReflectHelper.findGetterMethod( AnEntity.class, "pk" )
		);
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject( getter );

		final ObjectInputStream ois = new ObjectInputStream( new ByteArrayInputStream( baos.toByteArray() ) );

		final Getter getterClone = (Getter) ois.readObject();

		assertSame( getter.get( entity ), getterClone.get( entity ) );
	}

	@Test
	@JiraKey( value = "HHH-11202")
	public void testProtectedMethodSetter() throws Exception {
		final AnEntity entity = new AnEntity( new PK( 1L ) );

		final Getter getter = new GetterMethodImpl(
				AnEntity.class,
				"pk",
				ReflectHelper.findGetterMethod( AnEntity.class, "pk" )
		);
		final Setter setter = new SetterMethodImpl(
				AnEntity.class,
				"pk",
				ReflectHelper.findSetterMethod( AnEntity.class, "pk", PK.class )
		);

		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject( setter );

		final ObjectInputStream ois = new ObjectInputStream( new ByteArrayInputStream( baos.toByteArray() ) );

		final Setter setterClone = (Setter) ois.readObject();
		final PK pkNew = new PK( 2L );
		setterClone.set( entity, pkNew );

		assertSame( pkNew, getter.get( entity ) );
	}
}
