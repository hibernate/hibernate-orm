/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.criteria.JoinType;

import org.hibernate.envers.Audited;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.orm.test.envers.BaseEnversFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.junit.Test;

import org.hibernate.testing.orm.junit.JiraKey;

/**
 * @author Chris Cranford
 */
@JiraKey( "HHH-13817" )
public class AssociationRevisionsOfEntitiesQueryTest extends BaseEnversFunctionalTestCase {
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class<?>[] { Template.class, TemplateType.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		doInHibernate( this::sessionFactory, session -> {
			final TemplateType type1 = new TemplateType( 1, "Type1" );
			final TemplateType type2 = new TemplateType( 2, "Type2" );
			session.persist( type1 );
			session.persist( type2 );

			final Template template = new Template( 1, "Template1", type1 );
			session.persist( template );
		} );

		doInHibernate( this::sessionFactory, session -> {
			final TemplateType type = session.find( TemplateType.class, 2 );
			final Template template = session.find( Template.class, 1 );
			template.setName( "Template1-Updated" );
			template.setTemplateType( type );
			session.merge( template );
		} );

		doInHibernate( this::sessionFactory, session -> {
			final Template template = session.find( Template.class, 1 );
			session.remove( template );
		} );
	}

	@Test
	public void testRevisionsOfEntityWithAssociationQueries() {
		doInHibernate( this::sessionFactory, session -> {
			List<?> results = getAuditReader().createQuery()
					.forRevisionsOfEntity( Template.class, true, true )
					.add( AuditEntity.id().eq( 1 ) )
					.traverseRelation( "templateType", JoinType.INNER )
					.add( AuditEntity.property( "name" ).eq( "Type1" ) )
					.up()
					.getResultList();
			assertEquals( 1, results.size() );
			assertEquals( "Template1", ( (Template) results.get( 0 ) ).getName() );
		} );

		doInHibernate( this::sessionFactory, session -> {
			List<?> results = getAuditReader().createQuery()
					.forRevisionsOfEntity( Template.class, true, true )
					.add( AuditEntity.id().eq( 1 ) )
					.traverseRelation( "templateType", JoinType.INNER )
					.add( AuditEntity.property("name" ).eq("Type2" ) )
					.up()
					.getResultList();

			assertEquals( getConfiguration().isStoreDataAtDelete() ? 2 : 1, results.size() );
			for ( Object result : results ) {
				assertEquals( "Template1-Updated", ( (Template) result ).getName() );
			}
		} );
	}

	@Test
	public void testAssociationQueriesNotAllowedWhenNotSelectingJustEntities() {
		try {
			doInHibernate( this::sessionFactory, session -> {
				getAuditReader().createQuery()
						.forRevisionsOfEntity( Template.class, false, true )
						.add( AuditEntity.id().eq( 1 ) )
						.traverseRelation("templateType", JoinType.INNER )
						.add( AuditEntity.property( "name" ).eq( "Type1" ) )
						.up()
						.getResultList();
			} );

			fail( "Test should have thrown IllegalStateException due to selectEntitiesOnly=false" );
		}
		catch ( Exception e ) {
			assertTyping( IllegalStateException.class, e );
		}
	}

	@Entity(name = "TemplateType")
	@Audited
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
	@Audited
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
