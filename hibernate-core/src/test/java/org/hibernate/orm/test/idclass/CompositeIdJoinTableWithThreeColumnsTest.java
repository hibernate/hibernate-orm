package org.hibernate.orm.test.idclass;

import java.io.Serializable;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;

import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToOne;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Jira("https://hibernate.atlassian.net/browse/HHH-20961")
public class CompositeIdJoinTableWithThreeColumnsTest {

	@Test
	public void testCompositeIdWithThreeColumnsJoinTable() {
		try (StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry()) {
			final MetadataSources metadataSources = new MetadataSources(ssr)
					.addAnnotatedClass(Employee.class)
					.addAnnotatedClass(EmployeeBadge.class);
			assertDoesNotThrow(() -> metadataSources.buildMetadata());
		}
	}

	public static class EmployeeIds implements Serializable {
		public Long companyId;
		public Long departmentId;
		public Long employeeId;
	}

	@Entity(name = "Employee")
	@IdClass(EmployeeIds.class)
	public static class Employee {
		@Id
		public Long companyId;
		@Id
		public Long departmentId;
		@Id
		public Long employeeId;
		public String name;
		@OneToOne
		@JoinTable(joinColumns = {
				@JoinColumn(referencedColumnName = "companyId"),
				@JoinColumn(referencedColumnName = "departmentId"),
				@JoinColumn(referencedColumnName = "employeeId")
		}, inverseJoinColumns = {
				@JoinColumn(referencedColumnName = "badge_id1"),
				@JoinColumn(referencedColumnName = "badge_id2"),
				@JoinColumn(referencedColumnName = "badge_id3")
		})
		public EmployeeBadge badge;
	}

	public static class BadgeIds implements Serializable {
		public Long badge_id1;
		public Long badge_id2;
		public Long badge_id3;
	}

	@Entity(name = "EmployeeBadge")
	@IdClass(BadgeIds.class)
	public static class EmployeeBadge {
		@Id
		public Long badge_id1;
		@Id
		public Long badge_id2;
		@Id
		public Long badge_id3;
		public String badgeNumber;
	}
}
