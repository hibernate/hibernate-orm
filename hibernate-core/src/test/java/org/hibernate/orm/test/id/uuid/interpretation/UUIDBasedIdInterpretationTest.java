/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.uuid.interpretation;

import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.type.descriptor.jdbc.VarcharUUIDJdbcType;
import org.hibernate.testing.util.uuid.SafeRandomUUIDGenerator;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.UUIDJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.UUIDJdbcType;
import org.hibernate.type.descriptor.jdbc.VarbinaryJdbcType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = { UUIDBasedIdInterpretationTest.UuidIdEntity.class })
@SessionFactory
public class UUIDBasedIdInterpretationTest {

	@Test
	@JiraKey( "HHH-10564" )
	@RequiresDialect( H2Dialect.class )
	public void testH2(DomainModelScope scope) {
		checkUuidTypeUsed( scope, UUIDJdbcType.class );
	}

	@Test
	@JiraKey( "HHH-10564" )
	@RequiresDialect(value = MySQLDialect.class, matchSubTypes = false)
	public void testMySQL(DomainModelScope scope) {
		checkUuidTypeUsed( scope, VarbinaryJdbcType.class );
	}

	@Test
	@JiraKey( "HHH-10564" )
	@RequiresDialect(value = MariaDBDialect.class, majorVersion = 10, minorVersion = 7)
	public void testMariaDB(DomainModelScope scope) {
		checkUuidTypeUsed( scope, VarcharUUIDJdbcType.class );
	}

	@Test
	@JiraKey( "HHH-10564" )
	@RequiresDialect( value = PostgreSQLDialect.class )
	public void testPostgreSQL(DomainModelScope scope) {
		checkUuidTypeUsed( scope, UUIDJdbcType.class );
	}

	@Test
	@JiraKey( "HHH-10564" )
	@RequiresDialect(H2Dialect.class)
	public void testBinaryRuntimeUsage(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.byId( UuidIdEntity.class ).load( SafeRandomUUIDGenerator.safeRandomUUID() );
		} );
	}

	private void checkUuidTypeUsed(DomainModelScope scope, Class<? extends JdbcType> jdbcTypeDescriptor) {
		final PersistentClass entityBinding = scope.getDomainModel().getEntityBinding( UuidIdEntity.class.getName() );
		final BasicType<?> idPropertyType = (BasicType<?>) entityBinding.getIdentifier().getType();
		assertSame( UUIDJavaType.INSTANCE, idPropertyType.getJavaTypeDescriptor() );
		assertThat( idPropertyType.getJdbcType(), instanceOf( jdbcTypeDescriptor ) );
	}

	@Entity(name = "UuidIdEntity")
	@Table(name = "UUID_ID_ENTITY")
	public static class UuidIdEntity {
		@Id
		@GeneratedValue
		private UUID id;
	}
}
