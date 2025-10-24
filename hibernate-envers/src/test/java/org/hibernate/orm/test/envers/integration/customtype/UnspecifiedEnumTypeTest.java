/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.customtype;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.strategy.internal.DefaultAuditStrategy;
import org.hibernate.orm.test.envers.entities.customtype.UnspecifiedEnumTypeEntity;
import org.hibernate.testing.envers.RequiresAuditStrategy;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.StandardBasicTypes;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-7780")
@RequiresAuditStrategy(DefaultAuditStrategy.class)
@EnversTest
@DomainModel(xmlMappings = "mappings/customType/mappings.hbm.xml")
@ServiceRegistry(settings = {
		@Setting(name = AvailableSettings.SHOW_SQL, value = "true"),
		@Setting(name = AvailableSettings.FORMAT_SQL, value = "true"),
		@Setting(name = AvailableSettings.PREFER_NATIVE_ENUM_TYPES, value = "false")
})
@SessionFactory
public class UnspecifiedEnumTypeTest {
	private Long id = null;

	@BeforeClassTemplate
	public void initData(SessionFactoryScope scope) {
		// Revision 1
		this.id = scope.fromTransaction( session -> {
			UnspecifiedEnumTypeEntity entity = new UnspecifiedEnumTypeEntity(
					UnspecifiedEnumTypeEntity.E1.X,
					UnspecifiedEnumTypeEntity.E2.A
			);
			session.persist( entity );
			return entity.getId();
		} );

		// Revision 2
		scope.inTransaction( session -> {
			UnspecifiedEnumTypeEntity entity = session.get( UnspecifiedEnumTypeEntity.class, this.id );
			entity.setEnum1( UnspecifiedEnumTypeEntity.E1.Y );
			entity.setEnum2( UnspecifiedEnumTypeEntity.E2.B );
			session.merge( entity );
		} );
	}

	@Test
	public void testRevisionCount(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var auditReader = AuditReaderFactory.get( session );
			assertEquals(
					Arrays.asList( 1, 2 ),
					auditReader.getRevisions( UnspecifiedEnumTypeEntity.class, id )
			);
		} );
	}

	@Test
	public void testHistoryOfEnums(SessionFactoryScope scope) {
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

		scope.inSession( session -> {
			final var auditReader = AuditReaderFactory.get( session );
			assertEquals( ver1, auditReader.find( UnspecifiedEnumTypeEntity.class, id, 1 ) );
			assertEquals( ver2, auditReader.find( UnspecifiedEnumTypeEntity.class, id, 2 ) );
		} );
	}

	@Test
	public void testEnumRepresentation(SessionFactoryScope scope) {
		scope.inSession( session -> {
			@SuppressWarnings("unchecked")
			List<Object[]> values = session
					.createNativeQuery( "SELECT enum1 e1, enum2 e2 FROM ENUM_ENTITY_AUD ORDER BY REV ASC" )
					.addScalar( "e1", StandardBasicTypes.INTEGER )
					.addScalar( "e2", StandardBasicTypes.INTEGER )
					.list();

			assertNotNull( values );
			assertEquals( 2, values.size() );
			assertArrayEquals( new Object[] {0, 0}, values.get( 0 ) );
			assertArrayEquals( new Object[] {1, 1}, values.get( 1 ) );
		} );
	}
}
