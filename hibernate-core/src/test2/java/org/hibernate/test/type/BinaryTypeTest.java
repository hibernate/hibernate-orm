/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.Session;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad MIhalcea
 */
public class BinaryTypeTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {Image.class};
	}

	@Test
	public void testByteArrayStringRepresentation() {
		Session s = openSession();
		s.getTransaction().begin();
		try {
			Image image = new Image();
			image.id = 1L;
			image.content = new byte[] {1, 2, 3};

			s.save( image );
			s.getTransaction().commit();
		}
		catch (Exception e) {
			if ( s.getTransaction() != null && s.getTransaction().getStatus() == TransactionStatus.ACTIVE ) {
				s.getTransaction().rollback();
			}
			fail( e.getMessage() );
		}
		finally {
			s.close();
		}

		s = openSession();
		s.getTransaction().begin();
		try {
			assertArrayEquals( new byte[] {1, 2, 3}, s.find( Image.class, 1L ).content );
			s.getTransaction().commit();
		}
		catch (Exception e) {
			if ( s.getTransaction() != null && s.getTransaction().getStatus() == TransactionStatus.ACTIVE ) {
				s.getTransaction().rollback();
			}
			fail( e.getMessage() );
		}
		finally {
			s.close();
		}
	}

	@Entity(name = "Image")
	public static class Image {

		@Id
		private Long id;

		@Column(name = "content")
		private byte[] content;
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}
}
