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
public class CompositeIdJoinTableTest {

	@Test
	public void testCompositeIdJoinTable() {
		try (StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry()) {
			final MetadataSources metadataSources = new MetadataSources(ssr)
					.addAnnotatedClass(Person.class)
					.addAnnotatedClass(Passport.class);
			assertDoesNotThrow(() -> metadataSources.buildMetadata());
		}
	}

	public static class Ids implements Serializable {
		public Long idclass_id;
		public Long idclass_id2;
	}

	@Entity(name = "Person")
	@IdClass(Ids.class)
	public static class Person {
		@Id
		public Long idclass_id;
		@Id
		public Long idclass_id2;
		public String name;
		@OneToOne
		@JoinTable(joinColumns = {
				@JoinColumn(referencedColumnName = "idclass_id"),
				@JoinColumn(referencedColumnName = "idclass_id2")
		}, inverseJoinColumns = {
				@JoinColumn(referencedColumnName = "pa_id"),
				@JoinColumn(referencedColumnName = "pa_id2")
		})
		public Passport passport;
	}

	public static class PaIds implements Serializable {
		public Long pa_id;
		public Long pa_id2;
	}

	@Entity(name = "Passport")
	@IdClass(PaIds.class)
	public static class Passport {
		@Id
		public Long pa_id;
		@Id
		public Long pa_id2;
		public String number;
	}
}
