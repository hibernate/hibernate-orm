/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.components;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.orm.test.envers.entities.components.UniquePropsEntity;
import org.hibernate.orm.test.envers.entities.components.UniquePropsNotAuditedEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-6636")
@EnversTest
@DomainModel(xmlMappings = {
		"mappings/components/UniquePropsEntity.hbm.xml",
		"mappings/components/UniquePropsNotAuditedEntity.hbm.xml"
})
@SessionFactory
public class PropertiesGroupTest {
	private PersistentClass uniquePropsAudit = null;
	private PersistentClass uniquePropsNotAuditedAudit = null;
	private UniquePropsEntity entityRev1 = null;
	private UniquePropsNotAuditedEntity entityNotAuditedRev2 = null;

	@BeforeClassTemplate
	public void initData(SessionFactoryScope scope) {
		uniquePropsAudit = scope.getMetadataImplementor().getEntityBinding(
				"org.hibernate.orm.test.envers.entities.components.UniquePropsEntity_AUD"
		);
		uniquePropsNotAuditedAudit = scope.getMetadataImplementor().getEntityBinding(
				"org.hibernate.orm.test.envers.entities.components.UniquePropsNotAuditedEntity_AUD"
		);

		// Revision 1
		scope.inTransaction( session -> {
			UniquePropsEntity ent = new UniquePropsEntity();
			ent.setData1( "data1" );
			ent.setData2( "data2" );
			session.persist( ent );

			entityRev1 = new UniquePropsEntity( ent.getId(), ent.getData1(), ent.getData2() );
		} );

		// Revision 2
		scope.inTransaction( session -> {
			UniquePropsNotAuditedEntity entNotAud = new UniquePropsNotAuditedEntity();
			entNotAud.setData1( "data3" );
			entNotAud.setData2( "data4" );
			session.persist( entNotAud );

			entityNotAuditedRev2 = new UniquePropsNotAuditedEntity( entNotAud.getId(), entNotAud.getData1(), null );
		} );
	}

	@Test
	public void testAuditTableColumns() {
		assertNotNull( uniquePropsAudit.getTable().getColumn( new Column( "DATA1" ) ) );
		assertNotNull( uniquePropsAudit.getTable().getColumn( new Column( "DATA2" ) ) );

		assertNotNull( uniquePropsNotAuditedAudit.getTable().getColumn( new Column( "DATA1" ) ) );
		assertNull( uniquePropsNotAuditedAudit.getTable().getColumn( new Column( "DATA2" ) ) );
	}

	@Test
	public void testHistoryOfUniquePropsEntity(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var auditReader = AuditReaderFactory.get( session );
			assertEquals( entityRev1, auditReader.find( UniquePropsEntity.class, entityRev1.getId(), 1 ) );
		} );
	}

	@Test
	public void testHistoryOfUniquePropsNotAuditedEntity(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var auditReader = AuditReaderFactory.get( session );
			assertEquals(
					entityNotAuditedRev2,
					auditReader.find( UniquePropsNotAuditedEntity.class, entityNotAuditedRev2.getId(), 2 )
			);
		} );
	}
}
