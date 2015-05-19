/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.enums;

import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Id;

import org.hibernate.AnnotationException;
import org.hibernate.boot.MetadataSources;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
public class InvalidEnumeratedJavaTypeTest extends BaseUnitTestCase {
	@Test
	public void testInvalidMapping() {
		try {
			new MetadataSources( )
					.addAnnotatedClass( TheEntity.class )
					.buildMetadata();
			fail( "Was expecting failure" );
		}
		catch (AnnotationException ignore) {
		}
	}

	@Entity
	public static class TheEntity {
		@Id private Long id;
		@Enumerated private Boolean yesNo;
	}
}
