/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.CascadeType.ALL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.graph.GraphSemantic.FETCH;

/**
 * @author Wander Winkelhorst
 */

@JiraKey( "HHH-20253" )
@DomainModel(
		annotatedClasses = {
				HHH20253Test.CompanyEntity.class,
				HHH20253Test.CompanyEmails.class,
				HHH20253Test.Item.class
		}
)
@ServiceRegistry
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions( lazyLoading = true )
class HHH20253Test {

	@Test
	void testWithFetchGraph(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var sqlStatementInspector = scope.getCollectingStatementInspector();
			sqlStatementInspector.clear();
			var query = session.createQuery( "select e from Item e", Item.class )
					.setFetchSize( 10 );

			var graph = session.createEntityGraph( Item.class );
			graph.addAttributeNode("company");
			query.applyGraph( graph, FETCH );

			var resultList = query.list();
			assertThat( resultList ).isNotEmpty();
		} );
	}

	@BeforeEach
	void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {

					CompanyEntity company1 = new CompanyEntity();
					company1.setCompanyEmails( new CompanyEmails( company1 ) );
					session.persist( company1 );

					CompanyEntity company2 = new CompanyEntity();
					company2.setCompanyEmails( new CompanyEmails( company2 ) );
					company2.setParent( company1 );
					session.persist( company2 );

					Item item1 = new Item();
					item1.setCompany( company2 );
					session.persist( item1 );

					Item item2 = new Item();
					item2.setCompany( company1 );
					session.persist( item2 );

					session.flush();
					session.clear();
				}
		);
	}

	@AfterEach
	void clearTestData(SessionFactoryScope scope){
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity
	static class CompanyEntity {

		@Id
		@GeneratedValue
		@Column(name = "id", nullable = false)
		private Long id;

		@OneToOne(mappedBy = "company", cascade = ALL, orphanRemoval = true)
		private CompanyEmails companyEmails;

		@ManyToOne(fetch = FetchType.LAZY)
    	@JoinColumn(name = "parent_company_id")
		private CompanyEntity parent;

		public CompanyEntity getParent() {
			return parent;
		}

		public void setParent(CompanyEntity parent) {
			this.parent = parent;
		}

		@Override
		public String toString() {
			return "RootEntity#" + id;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public void setCompanyEmails(CompanyEmails companyEmails) {
			this.companyEmails = companyEmails;
		}
	}

	@Entity
	static class CompanyEmails {

		@Id
		@GeneratedValue
		@Column(name = "id", nullable = false)
		private Long id;

		@OneToOne
		@JoinColumn(name = "company_id")
		private CompanyEntity company;

		public CompanyEmails() {
		}

		public CompanyEmails(CompanyEntity company) {
			this.company = company;
		}

		public CompanyEntity getCompany() {
			return company;
		}

		public void setCompany(CompanyEntity company) {
			this.company = company;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "Item")
	static class Item {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@ManyToOne(fetch = FetchType.LAZY)
		private CompanyEntity company;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public CompanyEntity getCompany() {
			return company;
		}

		public void setCompany(CompanyEntity company) {
			this.company = company;
		}
	}
}
