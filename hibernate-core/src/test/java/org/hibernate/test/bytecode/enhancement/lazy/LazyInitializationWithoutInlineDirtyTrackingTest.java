/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.hibernate.bytecode.enhance.spi.UnloadedClass;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestContext;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Guillaume Smet
 */
@TestForIssue(jiraKey = "HHH-12633")
@RunWith(BytecodeEnhancerRunner.class)
@CustomEnhancementContext( {EnhancerTestContext.class, LazyInitializationWithoutInlineDirtyTrackingTest.NoInlineDirtyTrackingContext.class} )
public class LazyInitializationWithoutInlineDirtyTrackingTest extends BaseCoreFunctionalTestCase {

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ File.class };
	}

	@Test
	public void test() {
		doInHibernate( this::sessionFactory, s -> {
			File file = new File();
			file.setId( 1L );
			file.setName( "file" );
			file.setBytes( new byte[]{ 0 } );

			s.persist( file );
		} );

		doInHibernate( this::sessionFactory, s -> {
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
