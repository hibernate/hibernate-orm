/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metamodel.attributeInSuper;

import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.ManagedType;

import org.junit.Test;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;

import static org.junit.Assert.assertNotNull;

/**
 * @author Hardy Ferentschik
 */
public class EmbeddableInSuperClassTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				AbstractEntity.class, EmbeddableEntity.class, Entity.class
		};
	}

	@Test
	@TestForIssue(jiraKey = "HHH-6475")
	public void ensureAttributeForEmbeddableIsGeneratedInMappedSuperClass() {
		EmbeddableType<EmbeddableEntity> embeddableType = entityManagerFactory().getMetamodel()
				.embeddable( EmbeddableEntity.class );

		Attribute<?, ?> attribute = embeddableType.getAttribute( "foo" );
		assertNotNull( attribute );

		ManagedType<AbstractEntity> managedType = entityManagerFactory().getMetamodel().managedType( AbstractEntity.class );
		assertNotNull( managedType );

		attribute = managedType.getAttribute( "embedded" );
		assertNotNull( attribute );
	}
}