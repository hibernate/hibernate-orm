/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.embeddable;

import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertAttributeTypeInMetaModelFor;

/**
 * @author Hardy Ferentschik
 */
public class EmbeddableAccessTypeTest extends CompilationTest {
	@Test
	@WithClasses({ Base.class, EmbeddableEntity.class, IStuff.class, MyEntity.class, Stuff.class })
	public void testCorrectAccessTypeUsedForEmbeddable() {
		assertAttributeTypeInMetaModelFor(
				EmbeddableEntity.class,
				"stuffs",
				Stuff.class,
				"The target annotation set the type to Stuff"
		);
	}
}
