/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.lob;

import java.sql.Types;
import java.util.Arrays;
import java.util.Objects;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.junit.Assert.assertTrue;

/**
 * @author Armin Krezović (armin.krezovic at ziragroup dot com)
 */
@JiraKey(value = "HHH-16253")
public class LargeObjectMappingTest extends BaseEnversJPAFunctionalTestCase {

	@Entity
	@Audited
	public static class LargeObjectTestEntity {
		@Id
		@GeneratedValue
		private Integer id;

		@JdbcTypeCode(Types.CLOB)
		private String clob;

		@JdbcTypeCode(Types.BLOB)
		private byte[] blob;

		public LargeObjectTestEntity() {
		}

		public LargeObjectTestEntity(Integer id, String clob, byte[] blob) {
			this.id = id;
			this.clob = clob;
			this.blob = blob;
		}

		@Override
		public final boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}

			if ( !( o instanceof LargeObjectTestEntity that ) ) {
				return false;
			}

			return Objects.equals( id, that.id ) && Objects.equals(
					clob,
					that.clob
			) && Arrays.equals( blob, that.blob );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id, clob, Arrays.hashCode( blob ) );
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { LargeObjectTestEntity.class };
	}

	@Test
	public void testLobTypeMapping() {
		PersistentClass entityBinding = metadata().getEntityBinding( LargeObjectTestEntity.class.getName() + "_AUD" );

		Property blobProperty = entityBinding.getProperty( "blob" );
		Property clobProperty = entityBinding.getProperty( "clob" );

		assertTrue( blobProperty.isLob() );
		assertTrue( clobProperty.isLob() );
	}
}
