/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.query;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.criteria.JoinType;

import org.hibernate.envers.Audited;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-11981")
public class AssociationEntitiesModifiedQueryTest extends BaseEnversJPAFunctionalTestCase {
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

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { TemplateType.class, Template.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1
		doInJPA( this::entityManagerFactory, entityManager -> {
			final TemplateType type1 = new TemplateType( 1, "Type1" );
			final TemplateType type2 = new TemplateType( 2, "Type2" );
			final Template template = new Template( 1, "Template1", type1 );
			entityManager.persist( type1 );
			entityManager.persist( type2 );
			entityManager.persist( template );
		} );

		// Revision 2
		doInJPA( this::entityManagerFactory, entityManager -> {
			final TemplateType type = entityManager.find( TemplateType.class, 2 );
			final Template template = entityManager.find( Template.class, 1 );
			template.setTemplateType( type );
			entityManager.merge( template );
		} );

		// Revision 3
		doInJPA( this::entityManagerFactory, entityManager -> {
			final Template template = entityManager.find( Template.class, 1 );
			entityManager.remove( template );
		} );
	}

	@Test
	public void testEntitiesModifiedAtRevision1WithAssociationQueries() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			List results = getEntitiesModifiedAtRevisionUsingAssociationQueryResults( 1 );
			assertEquals( 1, results.size() );
			assertEquals( "Type1", ( (TemplateType) results.get( 0 ) ).getName() );
		} );
	}

	@Test
	public void testEntitiesModifiedAtRevision2WithAssociationQueries() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			List results = getEntitiesModifiedAtRevisionUsingAssociationQueryResults( 2 );
			assertEquals( 1, results.size() );
			assertEquals( "Type2", ( (TemplateType) results.get( 0 ) ).getName() );
		} );
	}

	@Test
	public void testEntitiesModifiedAtRevision3WithAssociationQueries() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			List results = getEntitiesModifiedAtRevisionUsingAssociationQueryResults( 3 );
			assertEquals( 0, results.size() );
		} );
	}

	private List getEntitiesModifiedAtRevisionUsingAssociationQueryResults(Number revision) {
		// Without fix HHH-11981, throw org.hibernate.QueryException - Parameter not bound : revision
		return getAuditReader().createQuery()
				.forEntitiesModifiedAtRevision( Template.class, revision )
				.traverseRelation( "templateType", JoinType.INNER )
				.addProjection( AuditEntity.selectEntity( false ) )
				.up()
				.add( AuditEntity.property( "templateType" ).hasChanged() )
				.getResultList();
	}
}
