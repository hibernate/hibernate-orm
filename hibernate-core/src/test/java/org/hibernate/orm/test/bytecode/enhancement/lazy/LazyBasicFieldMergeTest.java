/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@JiraKey("HHH-11117")
@DomainModel(
		annotatedClasses = {
				LazyBasicFieldMergeTest.Company.class,
				LazyBasicFieldMergeTest.Manager.class,
		}
)
@SessionFactory
@BytecodeEnhanced
public class LazyBasicFieldMergeTest {

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Manager manager = new Manager();
			manager.setName("John Doe");
			manager.setResume(new byte[] {1, 2, 3});

			Company company = new Company();
			company.setName("Company");
			company.setManager(manager);

			Company _company = (Company) session.merge( company);
			assertEquals( company.getName(), _company.getName() );
			assertArrayEquals( company.getManager().getResume(), _company.getManager().getResume() );
		} );
	}

	@Entity(name = "Company")
	@Table(name = "COMPANY")
	public static class Company {

		@Id
		@GeneratedValue
		@Column(name = "COMP_ID")
		private Long id;

		@Column(name = "NAME")
		private String name;

		@OneToOne(mappedBy = "company", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
		private Manager manager;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Manager getManager() {
			return manager;
		}

		public void setManager(Manager manager) {
			this.manager = manager;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}


	@Entity(name = "Manager")
	@Table(name = "MANAGER")
	public static class Manager {

		@Id
		@GeneratedValue
		@Column(name = "MAN_ID")
		private Long id;

		@Column(name = "NAME")
		private String name;

		@Lob
		@Column(name = "RESUME")
		@Basic(fetch = FetchType.LAZY)
		private byte[] resume;

		@OneToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "COMP_ID")
		private Company company;

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

		public byte[] getResume() {
			return resume;
		}

		public void setResume(byte[] resume) {
			this.resume = resume;
		}

		public Company getCompany() {
			return company;
		}

		public void setCompany(Company company) {
			this.company = company;
		}
	}
}
