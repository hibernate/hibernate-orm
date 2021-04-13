/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.metamodel.attributeInSuper;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.ManagedType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Hardy Ferentschik
 */
@Jpa(annotatedClasses = {
		AbstractEntity.class,
		EmbeddableEntity.class,
		Entity.class
})
public class EmbeddableInSuperClassTest {

	@Test
	@TestForIssue(jiraKey = "HHH-6475")
	public void ensureAttributeForEmbeddableIsGeneratedInMappedSuperClass(EntityManagerFactoryScope scope) {
		EmbeddableType<EmbeddableEntity> embeddableType = scope.getEntityManagerFactory().getMetamodel().embeddable( EmbeddableEntity.class );

		Attribute<?, ?> attribute = embeddableType.getAttribute( "foo" );
		assertNotNull( attribute );

		ManagedType<AbstractEntity> managedType = scope.getEntityManagerFactory().getMetamodel().managedType( AbstractEntity.class );
		assertNotNull( managedType );

		attribute = managedType.getAttribute( "embedded" );
		assertNotNull( attribute );
	}
}