/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.criteria.JoinType;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-11981")
@Jpa(annotatedClasses = {
		AssociationEntitiesModifiedQueryTest.TemplateType.class,
		AssociationEntitiesModifiedQueryTest.Template.class
})
@EnversTest
public class AssociationEntitiesModifiedQueryTest {
	@Entity(name = "TemplateType")
	@Audited(withModifiedFlag = true)
	public static class TemplateType {
		@Id
		private Integer id;
		private String name;

		TemplateType() {
			this( null, null );
		}

		TemplateType(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "Template")
	@Audited(withModifiedFlag = true)
	public static class Template {
		@Id
		private Integer id;
		private String name;
		@ManyToOne
		private TemplateType templateType;

		Template() {
			this( null, null, null );
		}

		Template(Integer id, String name, TemplateType type) {
			this.id = id;
			this.name = name;
			this.templateType = type;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public TemplateType getTemplateType() {
			return templateType;
		}

		public void setTemplateType(TemplateType templateType) {
			this.templateType = templateType;
		}
	}

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( entityManager -> {
			final TemplateType type1 = new TemplateType( 1, "Type1" );
			final TemplateType type2 = new TemplateType( 2, "Type2" );
			final Template template = new Template( 1, "Template1", type1 );
			entityManager.persist( type1 );
			entityManager.persist( type2 );
			entityManager.persist( template );
		} );

		// Revision 2
		scope.inTransaction( entityManager -> {
			final TemplateType type = entityManager.find( TemplateType.class, 2 );
			final Template template = entityManager.find( Template.class, 1 );
			template.setTemplateType( type );
			entityManager.merge( template );
		} );

		// Revision 3
		scope.inTransaction( entityManager -> {
			final Template template = entityManager.find( Template.class, 1 );
			entityManager.remove( template );
		} );
	}

	@Test
	public void testEntitiesModifiedAtRevision1WithAssociationQueries(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			List results = getEntitiesModifiedAtRevisionUsingAssociationQueryResults( entityManager, 1 );
			assertEquals( 1, results.size() );
			assertEquals( "Type1", ( (TemplateType) results.get( 0 ) ).getName() );
		} );
	}

	@Test
	public void testEntitiesModifiedAtRevision2WithAssociationQueries(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			List results = getEntitiesModifiedAtRevisionUsingAssociationQueryResults( entityManager, 2 );
			assertEquals( 1, results.size() );
			assertEquals( "Type2", ( (TemplateType) results.get( 0 ) ).getName() );
		} );
	}

	@Test
	public void testEntitiesModifiedAtRevision3WithAssociationQueries(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			List results = getEntitiesModifiedAtRevisionUsingAssociationQueryResults( entityManager, 3 );
			assertEquals( 0, results.size() );
		} );
	}

	private List getEntitiesModifiedAtRevisionUsingAssociationQueryResults(jakarta.persistence.EntityManager entityManager, Number revision) {
		// Without fix HHH-11981, throw org.hibernate.QueryException - Parameter not bound : revision
		return AuditReaderFactory.get( entityManager ).createQuery()
				.forEntitiesModifiedAtRevision( Template.class, revision )
				.traverseRelation( "templateType", JoinType.INNER )
				.addProjection( AuditEntity.selectEntity( false ) )
				.up()
				.add( AuditEntity.property( "templateType" ).hasChanged() )
				.getResultList();
	}
}
