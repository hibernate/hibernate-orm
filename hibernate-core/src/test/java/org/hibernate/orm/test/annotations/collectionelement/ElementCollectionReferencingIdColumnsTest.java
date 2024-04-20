package org.hibernate.orm.test.annotations.collectionelement;

import java.util.List;
import java.util.Set;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(
		annotatedClasses = {
				ElementCollectionReferencingIdColumnsTest.Company.class,
				ElementCollectionReferencingIdColumnsTest.Employee.class,
				ElementCollectionReferencingIdColumnsTest.Phone.class,
		}
)
@SessionFactory
@JiraKey("HHH-17695")
public class ElementCollectionReferencingIdColumnsTest {
	private static final Long COMPANY_ID = 1L;

	private static final Long EMPLOYEE_ID = 3l;
	private static final String USER_ID = "4";
	private static final Long EMPLOYEE_ID_2 = 5l;
	private static final String USER_ID_2 = "6";

	private static final Long PHONE_ID = 7l;
	private static final Long PHONE_ID_2 = 8l;

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {

					Phone phone = new Phone( PHONE_ID, "1234567", EMPLOYEE_ID );
					Phone phone2 = new Phone( PHONE_ID_2, "8910112", EMPLOYEE_ID_2 );

					Employee employee = new Employee( EMPLOYEE_ID, USER_ID, "and", COMPANY_ID );
					Employee employee2 = new Employee( EMPLOYEE_ID_2, USER_ID_2, "and", COMPANY_ID );
					Company company = new Company( COMPANY_ID, "acme" );

					session.persist( company );

					session.persist( employee );
					session.persist( employee2 );

					session.persist( phone );
					session.persist( phone2 );
				}
		);
	}

	@Test
	public void testSelect(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Company> companies = session.createQuery(
									"select c from Company c left join fetch c.employeeUserIds",
									Company.class
							)
							.getResultList();
					assertThat( companies.size() ).isEqualTo( 1 );
					Company company = companies.get( 0 );
					assertThat( company.getEmployeeUserIds().size() ).isEqualTo( 2 );
					assertTrue( company.getEmployeeUserIds().contains( USER_ID ) );
					assertTrue( company.getEmployeeUserIds().contains( USER_ID_2 ) );
				}
		);
	}

	@Entity(name = "Company")
	@Table(name = "COMPANY")
	public static class Company {

		@Id
		private Long id;

		private String name;

		@ElementCollection
		@CollectionTable(name = "EMPLOYEE",
				joinColumns = @JoinColumn(name = "COMPANY_ID"))
		@Column(name = "USER_ID")
		private Set<String> employeeUserIds;

		public Company() {
		}

		public Company(Long id, String name) {
			this.id = id;
			this.name = name;
		}

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

		public Set<String> getEmployeeUserIds() {
			return employeeUserIds;
		}

	}

	@Entity(name = "Employee")
	@Table(name = "EMPLOYEE")
	public static class Employee {

		@Id
		private Long id;

		@Column(name = "USER_ID")
		private String userId;

		private String name;

		@Column(name = "COMPANY_ID")
		private Long companyId;

		@ElementCollection
		@CollectionTable(name = "PHONE",
				joinColumns = @JoinColumn(name = "EMPLOYEE_ID"))
		@Column(name = "PHONE_NUMBER")
		private List<String> phoneNumbers;

		public Employee() {
		}

		public Employee(Long id, String userId, String name, Long companyId) {
			this.id = id;
			this.userId = userId;
			this.name = name;
			this.companyId = companyId;
		}

		public Long getId() {
			return id;
		}

		public String getUserId() {
			return userId;
		}

		public void setUserId(String userId) {
			this.userId = userId;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Long getCompanyId() {
			return companyId;
		}

		public void setCompanyId(Long companyId) {
			this.companyId = companyId;
		}

		public List<String> getPhoneNumbers() {
			return phoneNumbers;
		}
	}

	@Entity(name = "Phone")
	@Table(name = "PHONE")
	public static class Phone {

		@Id
		private Long id;

		@Column(name = "PHONE_NUMBER")
		private String phoneNumber;

		@Column(name = "EMPLOYEE_ID")
		private Long employeeId;

		public Long getId() {
			return id;
		}

		public Phone() {
		}

		public Phone(Long id, String phoneNumber, Long employeeId) {
			this.id = id;
			this.phoneNumber = phoneNumber;
			this.employeeId = employeeId;
		}

		public Long getEmployeeId() {
			return employeeId;
		}

		public void setEmployeeId(Long employeeId) {
			this.employeeId = employeeId;
		}

	}
}
