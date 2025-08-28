/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.test.integration.customtype;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.envers.strategy.internal.DefaultAuditStrategy;
import org.hibernate.orm.test.envers.BaseEnversFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.customtype.UnspecifiedEnumTypeEntity;
import org.hibernate.type.StandardBasicTypes;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.envers.RequiresAuditStrategy;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-7780")
@RequiresAuditStrategy(DefaultAuditStrategy.class)
public class UnspecifiedEnumTypeTest extends BaseEnversFunctionalTestCase {
	private Long id = null;

	@Override
	protected String[] getMappings() {
		return new String[]{ "mappings/customType/mappings.hbm.xml" };
	}

	@Override
	protected void addSettings(Map<String,Object> settings) {
		super.addSettings( settings );

		settings.put( AvailableSettings.SHOW_SQL, "true" );
		settings.put( AvailableSettings.FORMAT_SQL, "true" );
		settings.put( AvailableSettings.PREFER_NATIVE_ENUM_TYPES, "false" );
	}

	@Test
	@Priority(9)
	public void initData() {
		Session session = getSession();

		// Revision 1
		session.getTransaction().begin();
		UnspecifiedEnumTypeEntity entity = new UnspecifiedEnumTypeEntity(
				UnspecifiedEnumTypeEntity.E1.X,
				UnspecifiedEnumTypeEntity.E2.A
		);
		session.persist( entity );
		session.getTransaction().commit();

		id = entity.getId();

		// Revision 2
		session.getTransaction().begin();
		entity = session.get( UnspecifiedEnumTypeEntity.class, entity.getId() );
		entity.setEnum1( UnspecifiedEnumTypeEntity.E1.Y );
		entity.setEnum2( UnspecifiedEnumTypeEntity.E2.B );
		session.merge( entity );
		session.getTransaction().commit();

		session.close();
	}

	@Test
	@Priority(8)
	public void testRevisionCount() {
		Assert.assertEquals(
				Arrays.asList( 1, 2 ), getAuditReader().getRevisions(
				UnspecifiedEnumTypeEntity.class,
				id
		)
		);
	}

	@Test
	@Priority(7)
	public void testHistoryOfEnums() {
		UnspecifiedEnumTypeEntity ver1 = new UnspecifiedEnumTypeEntity(
				UnspecifiedEnumTypeEntity.E1.X,
				UnspecifiedEnumTypeEntity.E2.A,
				id
		);
		UnspecifiedEnumTypeEntity ver2 = new UnspecifiedEnumTypeEntity(
				UnspecifiedEnumTypeEntity.E1.Y,
				UnspecifiedEnumTypeEntity.E2.B,
				id
		);

		Assert.assertEquals( ver1, getAuditReader().find( UnspecifiedEnumTypeEntity.class, id, 1 ) );
		Assert.assertEquals( ver2, getAuditReader().find( UnspecifiedEnumTypeEntity.class, id, 2 ) );
	}

	@Test
	@Priority(6)
	public void testEnumRepresentation() {
		Session session = getSession();

		@SuppressWarnings("unchecked")
		List<Object[]> values = session
				.createNativeQuery( "SELECT enum1 e1, enum2 e2 FROM ENUM_ENTITY_AUD ORDER BY REV ASC" )
				.addScalar( "e1", StandardBasicTypes.INTEGER )
				.addScalar( "e2", StandardBasicTypes.INTEGER )
				.list();
		session.close();

		Assert.assertNotNull( values );
		Assert.assertEquals( 2, values.size() );
		Assert.assertArrayEquals( new Object[]{ 0, 0 }, values.get( 0 ) );
		Assert.assertArrayEquals( new Object[]{ 1, 1 }, values.get( 1 ) );
	}
}
