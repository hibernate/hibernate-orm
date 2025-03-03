/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import org.hibernate.bytecode.enhance.spi.UnloadedClass;

import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestContext;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume Smet
 */
@JiraKey("HHH-12633")
@DomainModel(
		annotatedClasses = {
				LazyInitializationWithoutInlineDirtyTrackingTest.File.class
		}
)
@SessionFactory
@BytecodeEnhanced
@CustomEnhancementContext( {EnhancerTestContext.class, LazyInitializationWithoutInlineDirtyTrackingTest.NoInlineDirtyTrackingContext.class} )
public class LazyInitializationWithoutInlineDirtyTrackingTest {

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			File file = new File();
			file.setId( 1L );
			file.setName( "file" );
			file.setBytes( new byte[]{ 0 } );

			s.persist( file );
		} );

		scope.inTransaction( s -> {
			File file = s.find( File.class, 1L );
			file.setBytes( new byte[]{ 1 } );
			s.persist( file );
		} );
	}

	// --- //

	@Entity
	@Table(name = "T_FILE")
	public static class File {

		@Id
		private Long id;

		private String name;

		@Column(name = "bytes")
		@Lob
		@Basic(fetch = FetchType.LAZY)
		private byte[] bytes;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public byte[] getBytes() {
			return bytes;
		}

		public void setBytes(byte[] bytes) {
			this.bytes = bytes;
		}
	}

	public static class NoInlineDirtyTrackingContext extends EnhancerTestContext {

		@Override
		public boolean doDirtyCheckingInline(UnloadedClass classDescriptor) {
			return false;
		}
	}
}
