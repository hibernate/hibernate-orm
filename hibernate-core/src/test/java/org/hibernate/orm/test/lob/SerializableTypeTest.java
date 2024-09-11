/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.lob;

import org.hibernate.dialect.SybaseASEDialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/**
 * Tests of {@link org.hibernate.type.SerializableType}
 *
 * @author Steve Ebersole
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/lob/SerializableMappings.hbm.xml"
)
@SessionFactory
public class SerializableTypeTest {

	@Test
	@SkipForDialect(dialectClass = SybaseASEDialect.class, reason = "HHH-6425")
	public void testNewSerializableType(SessionFactoryScope scope) {
		final String initialPayloadText = "Initial payload";
		final String changedPayloadText = "Changed payload";
		final String empty = "";

		SerializableHolder serializableHolder = scope.fromTransaction(
				session -> {
					SerializableHolder holder = new SerializableHolder();
					session.persist( holder );
					return holder;
				}
		);

		Long id = serializableHolder.getId();

		scope.inTransaction(
				session -> {
					SerializableHolder holder = session.get( SerializableHolder.class, id );
					assertNull( holder.getSerialData() );
					holder.setSerialData( new SerializableData( initialPayloadText ) );
				}
		);

		scope.inTransaction(
				session -> {
					SerializableHolder holder = session.get( SerializableHolder.class, id );
					SerializableData serialData = (SerializableData) holder.getSerialData();
					assertEquals( initialPayloadText, serialData.getPayload() );
					holder.setSerialData( new SerializableData( changedPayloadText ) );
				}
		);

		scope.inTransaction(
				session -> {
					SerializableHolder holder = session.get( SerializableHolder.class, id );
					SerializableData serialData = (SerializableData) holder.getSerialData();
					assertEquals( changedPayloadText, serialData.getPayload() );
					holder.setSerialData( null );
				}
		);

		scope.inTransaction(
				session -> {
					SerializableHolder holder = session.get( SerializableHolder.class, id );
					assertNull( holder.getSerialData() );
					holder.setSerialData( new SerializableData( empty ) );
				}
		);

		scope.inTransaction(
				session -> {
					SerializableHolder holder = session.get( SerializableHolder.class, id );
					SerializableData serialData = (SerializableData) holder.getSerialData();
					assertEquals( empty, serialData.getPayload() );
					session.remove( holder );
				}
		);
	}

}
