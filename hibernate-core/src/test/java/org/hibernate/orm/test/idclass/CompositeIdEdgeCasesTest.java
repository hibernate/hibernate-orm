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
public class CompositeIdEdgeCasesTest {

	@Test
	public void testSingleColumnIdWithJoinTable() {
		try (StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry()) {
			final MetadataSources metadataSources = new MetadataSources(ssr)
					.addAnnotatedClass(SingleIdPerson.class)
					.addAnnotatedClass(SingleIdPassport.class);
			assertDoesNotThrow(() -> metadataSources.buildMetadata());
		}
	}

	@Test
	public void testCompositeIdWithExplicitJoinColumns() {
		try (StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry()) {
			final MetadataSources metadataSources = new MetadataSources(ssr)
					.addAnnotatedClass(ExplicitPerson.class)
					.addAnnotatedClass(ExplicitPassport.class);
			assertDoesNotThrow(() -> metadataSources.buildMetadata());
		}
	}

	@Test
	public void testCompositeIdWithNamedColumns() {
		try (StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry()) {
			final MetadataSources metadataSources = new MetadataSources(ssr)
					.addAnnotatedClass(NamedColumnPerson.class)
					.addAnnotatedClass(NamedColumnPassport.class);
			assertDoesNotThrow(() -> metadataSources.buildMetadata());
		}
	}

	@Entity(name = "SingleIdPerson")
	public static class SingleIdPerson {
		@Id
		public Long id;
		public String name;
		@OneToOne
		@JoinTable(joinColumns = {
				@JoinColumn(referencedColumnName = "id")
		}, inverseJoinColumns = {
				@JoinColumn(referencedColumnName = "passportId")
		})
		public SingleIdPassport passport;
	}

	@Entity(name = "SingleIdPassport")
	public static class SingleIdPassport {
		@Id
		public Long passportId;
		public String number;
	}

	public static class ExplicitIds implements Serializable {
		public Long id1;
		public Long id2;
	}

	@Entity(name = "ExplicitPerson")
	@IdClass(ExplicitIds.class)
	public static class ExplicitPerson {
		@Id
		public Long id1;
		@Id
		public Long id2;
		public String name;
		@OneToOne
		@JoinTable(
				name = "person_passport_link",
				joinColumns = {
						@JoinColumn(name = "person_id1", referencedColumnName = "id1"),
						@JoinColumn(name = "person_id2", referencedColumnName = "id2")
				},
				inverseJoinColumns = {
						@JoinColumn(name = "passport_id1", referencedColumnName = "passId1"),
						@JoinColumn(name = "passport_id2", referencedColumnName = "passId2")
				}
		)
		public ExplicitPassport passport;
	}

	public static class ExplicitPassportIds implements Serializable {
		public Long passId1;
		public Long passId2;
	}

	@Entity(name = "ExplicitPassport")
	@IdClass(ExplicitPassportIds.class)
	public static class ExplicitPassport {
		@Id
		public Long passId1;
		@Id
		public Long passId2;
		public String number;
	}

	public static class NamedIds implements Serializable {
		public Long firstId;
		public Long secondId;
	}

	@Entity(name = "NamedColumnPerson")
	@IdClass(NamedIds.class)
	public static class NamedColumnPerson {
		@Id
		public Long firstId;
		@Id
		public Long secondId;
		public String name;
		@OneToOne
		@JoinTable(joinColumns = {
				@JoinColumn(referencedColumnName = "firstId"),
				@JoinColumn(referencedColumnName = "secondId")
		}, inverseJoinColumns = {
				@JoinColumn(referencedColumnName = "docId1"),
				@JoinColumn(referencedColumnName = "docId2")
		})
		public NamedColumnPassport passport;
	}

	public static class NamedPassportIds implements Serializable {
		public Long docId1;
		public Long docId2;
	}

	@Entity(name = "NamedColumnPassport")
	@IdClass(NamedPassportIds.class)
	public static class NamedColumnPassport {
		@Id
		public Long docId1;
		@Id
		public Long docId2;
		public String number;
	}
}
