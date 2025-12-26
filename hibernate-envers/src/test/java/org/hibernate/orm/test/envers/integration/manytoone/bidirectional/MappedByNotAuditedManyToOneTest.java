/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.manytoone.bidirectional;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.cfg.Configuration;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.boot.EnversMappingException;

import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

@Jira("https://hibernate.atlassian.net/browse/HHH-19897")
public class MappedByNotAuditedManyToOneTest {
	@Test
	public void testMappingException() {
		final Configuration cfg = new Configuration().addAnnotatedClasses( ToOneParent.class, ToOneChild.class );
		ServiceRegistryUtil.applySettings( cfg.getStandardServiceRegistryBuilder() );
		final var enversMappingException = assertThrows(
				"Expecting EnversMappingException to be thrown due to @NotAudited on owning side of bidirectional association",
				EnversMappingException.class,
				cfg::buildSessionFactory
		);
		assertThat( enversMappingException ).hasMessageContainingAll(
				"Could not resolve mapped by property for association",
				"parents"
		);
	}

	@Entity(name = "ToOneEntity")
	@Audited
	static class ToOneParent {
		@Id
		private Long id;

		private String parentData;

		@ManyToOne
		@NotAudited
		private ToOneChild child;
	}

	@Entity(name = "ToOneChild")
	@Audited
	static class ToOneChild {
		@Id
		private Long id;

		private String childData;

		@OneToMany(mappedBy = "child")
		private List<ToOneParent> parents;
	}
}
