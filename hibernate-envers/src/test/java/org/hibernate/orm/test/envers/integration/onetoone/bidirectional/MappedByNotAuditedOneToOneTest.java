/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetoone.bidirectional;

import org.hibernate.cfg.Configuration;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.boot.EnversMappingException;

import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

@Jira("https://hibernate.atlassian.net/browse/HHH-19897")
public class MappedByNotAuditedOneToOneTest {
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
				"Could not resolve mapped by property [child] for association",
				"parent"
		);
	}

	@Entity(name = "ToOneEntity")
	@Audited
	static class ToOneParent {
		@Id
		private Long id;

		private String parentData;

		@OneToOne
		@NotAudited
		private ToOneChild child;
	}

	@Entity(name = "ToOneChild")
	@Audited
	static class ToOneChild {
		@Id
		private Long id;

		private String childData;

		@OneToOne(mappedBy = "child")
		private ToOneParent parent;
	}
}
