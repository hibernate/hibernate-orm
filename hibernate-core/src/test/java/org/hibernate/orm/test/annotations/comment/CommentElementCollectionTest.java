/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.comment;


import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;
import org.hibernate.annotations.Comment;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey(value = "HHH-19011")
@DomainModel(annotatedClasses = {CommentElementCollectionTest.MainEntity.class})
@SessionFactory
@BootstrapServiceRegistry(integrators = CommentElementCollectionTest.MetadataIntegrator.class)
public class CommentElementCollectionTest {
	private static Metadata METADATA;

	@Test
	public void testTableCommentsPlacement(SessionFactoryScope scope) {
		scope.inSession(session -> {
			Collection<PersistentClass> entityBindings = METADATA.getEntityBindings();
			assertThat( entityBindings.iterator().next().getTable().getComment() ).isEqualTo(
					MainEntity.EXPECTED_MAIN_ENTITY_TABLE_COMMENT );

			Collection<org.hibernate.mapping.Collection> collectionBindings = METADATA.getCollectionBindings();
			assertThat( collectionBindings.iterator().next().getCollectionTable().getComment() ).isEqualTo(
					MainEntity.EXPECTED_ELEMENT_COLLECTION_TABLE_COMMENT );
		});
	}

	@Entity
	@Comment(MainEntity.EXPECTED_MAIN_ENTITY_TABLE_COMMENT)
	static class MainEntity {
		public static final String EXPECTED_MAIN_ENTITY_TABLE_COMMENT = "Expected main entity table level comment";
		public static final String EXPECTED_ELEMENT_COLLECTION_TABLE_COMMENT = "Expected element collection table level comment";

		@Id
		@GeneratedValue
		public long id;

		@ElementCollection
		@OrderColumn(name = "elementOrder", nullable = false)
		@Comment(EXPECTED_ELEMENT_COLLECTION_TABLE_COMMENT)
		public List<String> embeddableValues;
	}

	public static class MetadataIntegrator implements Integrator {
		@Override
		public void integrate(Metadata metadata, BootstrapContext bootstrapContext, SessionFactoryImplementor sessionFactory) {
			METADATA = metadata;
		}

		@Override
		public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
			// do nothing
		}
	}
}
