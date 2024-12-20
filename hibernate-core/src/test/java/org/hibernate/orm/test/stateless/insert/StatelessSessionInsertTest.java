/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stateless.insert;

import org.hibernate.cfg.MappingSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author mukhanov@gmail.com
 */
@ServiceRegistry(settings = @Setting(name = MappingSettings.TRANSFORM_HBM_XML, value = "true"))
@DomainModel(xmlMappings = "org/hibernate/orm/test/stateless/insert/Mappings.hbm.xml")
@SessionFactory
public class StatelessSessionInsertTest {

	@Test
	public void testInsertWithForeignKey(SessionFactoryScope scope) {
		Message msg = new Message();
		scope.inTransaction(
				session -> {
					final String messageId = "message_id";
					msg.setId( messageId );
					msg.setContent( "message_content" );
					msg.setSubject( "message_subject" );
					session.persist( msg );
				}
		);

		scope.inStatelessTransaction(
				statelessSession -> {
					MessageRecipient signature = new MessageRecipient();
					signature.setId( "recipient" );
					signature.setEmail( "recipient@hibernate.org" );
					signature.setMessage( msg );
					statelessSession.insert( signature );
				}
		);
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete MessageRecipient" ).executeUpdate();
					session.createQuery( "delete Message" ).executeUpdate();
				}
		);
	}
}
