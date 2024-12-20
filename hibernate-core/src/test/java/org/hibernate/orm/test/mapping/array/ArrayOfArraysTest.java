/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.array;

import org.hibernate.MappingException;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.type.SqlTypes;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.assertj.core.data.Index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Marco Belladelli
 */
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsStandardArrays.class )
public class ArrayOfArraysTest {
	@DomainModel( annotatedClasses = ArrayOfArraysTest.EntityWithDoubleByteArray.class )
	@SessionFactory
	@ServiceRegistry( settings = @Setting( name = AvailableSettings.HBM2DDL_AUTO, value = "create-drop" ) )
	@Test
	@SkipForDialect( dialectClass = CockroachDialect.class, reason = "Unable to find server array type for provided name bytes" )
	public void testDoubleByteArrayWorks(SessionFactoryScope scope) {
		final Long id = scope.fromTransaction( session -> {
			final EntityWithDoubleByteArray entity = new EntityWithDoubleByteArray();
			entity.setByteArray( new byte[][] { new byte[] { 1 } } );
			session.persist( entity );
			return entity.getId();
		} );
		scope.inSession( session -> {
			final byte[][] byteArray = session.find( EntityWithDoubleByteArray.class, id ).getByteArray();
			assertThat( byteArray ).hasDimensions( 1, 1 ).contains( new byte[] { 1 }, Index.atIndex( 0 ) );
		} );
	}

	@Test
	public void testDoubleIntegerArrayThrows() {
		final Configuration cfg = new Configuration();
		cfg.addAnnotatedClass( EntityWithDoubleIntegerArray.class );
		try (final org.hibernate.SessionFactory sf = cfg.buildSessionFactory()) {
			fail( "Expecting Integer[][] to trigger exception as non-byte multidimensional arrays are not supported" );
		}
		catch (Exception e) {
			assertThat( e ).isInstanceOf( MappingException.class ).hasMessage( "Nested arrays (with the exception of byte[][]) are not supported" );
		}
	}

	@Entity( name = "EntityWithDoubleByteArray" )
	static class EntityWithDoubleByteArray {
		@Id
		@GeneratedValue
		private Long id;

		@JdbcTypeCode( SqlTypes.ARRAY )
		private byte[][] byteArray;

		public Long getId() {
			return id;
		}

		public byte[][] getByteArray() {
			return byteArray;
		}

		public void setByteArray(byte[][] byteArray) {
			this.byteArray = byteArray;
		}
	}

	@Entity( name = "EntityWithDoubleIntegerArray" )
	static class EntityWithDoubleIntegerArray {
		@Id
		@GeneratedValue
		private Long id;

		@JdbcTypeCode( SqlTypes.ARRAY )
		private Integer[][] integers;
	}
}
