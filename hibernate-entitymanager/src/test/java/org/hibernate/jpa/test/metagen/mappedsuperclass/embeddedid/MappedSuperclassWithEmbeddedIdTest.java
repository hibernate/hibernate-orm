/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metagen.mappedsuperclass.embeddedid;

import javax.persistence.EntityManagerFactory;
import java.util.Arrays;

import org.hibernate.jpa.test.TestingEntityManagerFactoryGenerator;
import org.hibernate.jpa.AvailableSettings;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertNotNull;

/**
 * @author Steve Ebersole
 */
public class MappedSuperclassWithEmbeddedIdTest extends BaseUnitTestCase {
	@Test
	@TestForIssue( jiraKey = "HHH-5024" )
	public void testStaticMetamodel() {
		EntityManagerFactory emf = TestingEntityManagerFactoryGenerator.generateEntityManagerFactory(
				AvailableSettings.LOADED_CLASSES,
				Arrays.asList( Product.class )
		);

		assertNotNull( "'Product_.description' should not be null)", Product_.description );
		assertNotNull( "'Product_.id' should not be null)", Product_.id );

		assertNotNull( "'AbstractProduct_.id' should not be null)", AbstractProduct_.id );

		assertNotNull( "'ProductId_.id' should not be null)", ProductId_.id );
		assertNotNull( "'ProductId_.code' should not be null)", ProductId_.code );

		emf.close();
	}
}
