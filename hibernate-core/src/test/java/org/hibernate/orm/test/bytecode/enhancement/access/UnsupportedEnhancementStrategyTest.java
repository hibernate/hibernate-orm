/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.access;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImpl;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.EnhancementException;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.enhance.spi.UnsupportedEnhancementStrategy;
import org.hibernate.bytecode.internal.bytebuddy.ByteBuddyState;
import org.hibernate.bytecode.spi.ByteCodeHelper;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestContext;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JiraKey("HHH-18833")
public class UnsupportedEnhancementStrategyTest {

	@Test
	public void skip() throws IOException {
		var context = new EnhancerTestContext() {
			@Override
			public UnsupportedEnhancementStrategy getUnsupportedEnhancementStrategy() {
				// This is currently the default, but we don't care about what's the default here
				return UnsupportedEnhancementStrategy.SKIP;
			}
		};
		byte[] originalBytes = getAsBytes( SomeEntity.class );
		byte[] enhancedBytes = doEnhance( SomeEntity.class, originalBytes, context );
		assertThat( enhancedBytes ).isNull(); // null means "not enhanced"
	}

	@Test
	public void fail() throws IOException {
		var context = new EnhancerTestContext() {
			@Override
			public UnsupportedEnhancementStrategy getUnsupportedEnhancementStrategy() {
				return UnsupportedEnhancementStrategy.FAIL;
			}
		};
		byte[] originalBytes = getAsBytes( SomeEntity.class );
		assertThatThrownBy( () -> doEnhance( SomeEntity.class, originalBytes, context ) )
				.isInstanceOf( EnhancementException.class )
				.hasMessageContainingAll(
						String.format(
								"Enhancement of [%s] failed because no field named [%s] could be found for property accessor method [%s].",
								SomeEntity.class.getName(), "propertyMethod", "getPropertyMethod" ),
						"To fix this, make sure all property accessor methods have a matching field."
				);
	}

	@Test
	@SuppressWarnings("deprecation")
	public void legacy() throws IOException {
		var context = new EnhancerTestContext() {
			@Override
			public UnsupportedEnhancementStrategy getUnsupportedEnhancementStrategy() {
				// This is currently the default, but we don't care about what's the default here
				return UnsupportedEnhancementStrategy.LEGACY;
			}
		};
		byte[] originalBytes = getAsBytes( SomeEntity.class );
		byte[] enhancedBytes = doEnhance( SomeEntity.class, originalBytes, context );
		assertThat( enhancedBytes ).isNotNull(); // non-null means enhancement _was_ performed
	}

	private byte[] doEnhance(Class<SomeEntity> someEntityClass, byte[] originalBytes, EnhancementContext context) {
		final ByteBuddyState byteBuddyState = new ByteBuddyState();
		final Enhancer enhancer = new EnhancerImpl( context, byteBuddyState );
		return enhancer.enhance( someEntityClass.getName(), originalBytes );
	}

	private byte[] getAsBytes(Class<?> clazz) throws IOException {
		final String classFile = clazz.getName().replace( '.', '/' ) + ".class";
		try (InputStream classFileStream = clazz.getClassLoader().getResourceAsStream( classFile )) {
			return ByteCodeHelper.readByteCode( classFileStream );
		}
	}

	@Entity
	@Table(name = "SOME_ENTITY")
	static class SomeEntity {
		@Id
		Long id;

		@Basic
		String field;

		String property;

		public SomeEntity() {
		}

		public SomeEntity(Long id, String field, String property) {
			this.id = id;
			this.field = field;
			this.property = property;
		}

		/**
		 * The following property accessor methods are purposely named incorrectly to
		 * not match the "property" field.  The HHH-16572 change ensures that
		 * this entity is not (bytecode) enhanced.  Eventually further changes will be made
		 * such that this entity is enhanced in which case the FailureExpected can be removed
		 * and the cleanup() uncommented.
		 */
		@Basic
		@Access(AccessType.PROPERTY)
		public String getPropertyMethod() {
			return "from getter: " + property;
		}

		public void setPropertyMethod(String property) {
			this.property = property;
		}
	}
}
