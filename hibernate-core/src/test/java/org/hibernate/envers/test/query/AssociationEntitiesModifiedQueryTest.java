/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.query;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.criteria.JoinType;

import org.hibernate.envers.Audited;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-11981")
public class AssociationEntitiesModifiedQueryTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { TemplateType.class, Template.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		// Revision 1
		inTransaction(
				entityManager -> {
					final TemplateType type1 = new TemplateType( 1, "Type1" );
					final TemplateType type2 = new TemplateType( 2, "Type2" );
					final Template template = new Template( 1, "Template1", type1 );
					entityManager.persist( type1 );
					entityManager.persist( type2 );
					entityManager.persist( template );
				}
		);

		// Revision 2
		inTransaction( entityManager -> {
					final TemplateType type = entityManager.find( TemplateType.class, 2 );
					final Template template = entityManager.find( Template.class, 1 );
					template.setTemplateType( type );
					entityManager.merge( template );
				}
		);

		// Revision 3
		inTransaction(
				entityManager -> {
					final Template template = entityManager.find( Template.class, 1 );
					entityManager.remove( template );
				}
		);
	}

	@DynamicTest
	@Disabled("NYI - visitQualifiedEntityJoin")
	public void testEntitiesModifiedAtRevision1WithAssociationQueries() {
		inTransaction(
				entityManager -> {
					List results = getEntitiesModifiedAtRevisionUsingAssociationQueryResults( 1 );
					assertThat( results, CollectionMatchers.hasSize( 1 ) );
					assertThat( ( (TemplateType) results.get( 0 ) ).getName(), equalTo( "Type1" ) );
				}
		);
	}

	@DynamicTest
	@Disabled("NYI - visitQualifiedEntityJoin")
	public void testEntitiesModifiedAtRevision2WithAssociationQueries() {
		inTransaction(
				entityManager -> {
					List results = getEntitiesModifiedAtRevisionUsingAssociationQueryResults( 2 );
					assertThat( results, CollectionMatchers.hasSize( 1 ) );
					assertThat( ( (TemplateType) results.get( 0 ) ).getName(), equalTo( "Type2" ) );
				}
		);
	}

	@DynamicTest
	@Disabled("NYI - visitQualifiedEntityJoin")
	public void testEntitiesModifiedAtRevision3WithAssociationQueries() {
		inTransaction(
				entityManager -> {
					List results = getEntitiesModifiedAtRevisionUsingAssociationQueryResults( 3 );
					assertThat( results, CollectionMatchers.isEmpty() );
				}
		);
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
}
