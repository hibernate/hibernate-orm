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
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Transient;

import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImpl;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.EnhancementException;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.enhance.spi.UnsupportedEnhancementStrategy;
import org.hibernate.bytecode.internal.bytebuddy.ByteBuddyState;
import org.hibernate.bytecode.spi.ByteCodeHelper;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestContext;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

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
		byte[] enhancedBytes = doEnhance( SomeEntity.class, context );
		assertThat( enhancedBytes ).isNull(); // null means "not enhanced"
	}

	@Test
	public void fail() throws IOException {
		var context = new UnsupportedEnhancerContext();
		assertThatThrownBy( () -> doEnhance( SomeEntity.class, context ) ).isInstanceOf( EnhancementException.class )
				.hasMessageContainingAll(
						String.format(
								"Enhancement of [%s] failed because no field named [%s] could be found for property accessor method [%s].",
								SomeEntity.class.getName(),
								"propertyMethod",
								"getPropertyMethod"
						), "To fix this, make sure all property accessor methods have a matching field."
				);
		assertThatThrownBy( () -> doEnhance( SomeOtherEntity.class, context ) ).isInstanceOf( EnhancementException.class )
				.hasMessageContainingAll(
					String.format(
							"Enhancement of [%s] failed because no field named [%s] could be found for property accessor method [%s].",
							SomeOtherEntity.class.getName(),
							"propertyMethod",
							"setPropertyMethod"
					), "To fix this, make sure all property accessor methods have a matching field."
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
		byte[] enhancedBytes = doEnhance( SomeEntity.class, context );
		assertThat( enhancedBytes ).isNotNull(); // non-null means enhancement _was_ performed
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-18903" )
	@Jira( "https://hibernate.atlassian.net/browse/HHH-18904" )
	public void testEntityListeners() throws IOException {
		// non-null means check passed and enhancement _was_ performed
		assertThat( doEnhance( EventListenersEntity.class, new UnsupportedEnhancerContext() ) ).isNotNull();
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-18903" )
	@Jira( "https://hibernate.atlassian.net/browse/HHH-18904" )
	public void testAccessTypeFieldEntity() throws IOException {
		var context = new UnsupportedEnhancerContext();
		// non-null means check passed and enhancement _was_ performed
		assertThat( doEnhance( ExplicitAccessTypeFieldEntity.class, context ) ).isNotNull();
		assertThat( doEnhance( ImplicitAccessTypeFieldEntity.class, context ) ).isNotNull();
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-19059" )
	public void testAccessTypePropertyInherited() throws IOException {
		var context = new UnsupportedEnhancerContext();
		// non-null means check passed and enhancement _was_ performed
		assertThat( doEnhance( PropertyAccessInheritedEntity.class, context ) ).isNotNull();
	}

	private static byte[] doEnhance(Class<?> entityClass, EnhancementContext context) throws IOException {
		final ByteBuddyState byteBuddyState = new ByteBuddyState();
		final Enhancer enhancer = new EnhancerImpl( context, byteBuddyState );
		return enhancer.enhance( entityClass.getName(), getAsBytes( entityClass ) );
	}

	private static byte[] getAsBytes(Class<?> clazz) throws IOException {
		final String classFile = clazz.getName().replace( '.', '/' ) + ".class";
		try (InputStream classFileStream = clazz.getClassLoader().getResourceAsStream( classFile )) {
			return ByteCodeHelper.readByteCode( classFileStream );
		}
	}

	static class UnsupportedEnhancerContext extends EnhancerTestContext {
		@Override
		public UnsupportedEnhancementStrategy getUnsupportedEnhancementStrategy() {
			return UnsupportedEnhancementStrategy.FAIL;
		}
	}

	@Entity(name = "SomeEntity")
	static class SomeEntity {
		@Id
		Long id;

		@Basic
		String field;

		String property;

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

	@Entity(name = "SomeOtherEntity")
	static class SomeOtherEntity {
		@Id
		Long id;

		@Basic
		String field;

		String property;
		@Access(AccessType.PROPERTY)
		public void setPropertyMethod(String property) {
			this.property = property;
		}
	}

	@Entity(name = "EventListenersEntity")
	static class EventListenersEntity {
		private UUID id;

		private String status = "new";

		@Id
		public UUID getId() {
			return id;
		}

		public void setId(UUID id) {
			this.id = id;
		}

		// special case, we should let it through
		public UUID get() {
			return id;
		}

		@PrePersist
		public void setId() {
			id = UUID.randomUUID();
		}

		@Transient
		public String getState() {
			return status;
		}

		@PostLoad
		public void setState(String state) {
			status = "loaded";
		}

		@Transient
		public boolean isLoaded() {
			return status.equals( "loaded" );
		}
	}

	@Entity(name = "ExplicitAccessTypeFieldEntity")
	@Access( AccessType.FIELD )
	static class ExplicitAccessTypeFieldEntity {
		@Id
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Long get() {
			return id;
		}

		public String getSomething() {
			return "something";
		}
	}

	@Entity(name = "ImplicitAccessTypeFieldEntity")
	static class ImplicitAccessTypeFieldEntity {
		@Id
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Long get() {
			return id;
		}

		public String getAnother() {
			return "another";
		}
	}

	@MappedSuperclass
	static abstract class AbstractSuperclass {
		protected String property;
	}

	@Entity(name="PropertyAccessInheritedEntity")
	static class PropertyAccessInheritedEntity extends AbstractSuperclass {
		private Long id;

		@Id
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getProperty() {
			return property;
		}

		public void setProperty(String property) {
			this.property = property;
		}
	}
}
