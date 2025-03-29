/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.annotations.methods;

import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.annotations.AnEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = AnEntity.class )
@SessionFactory( generateStatistics = true )
public class SessionFactoryTesting {
	@Test
	public void testClassLevelAnnotations(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		assertThat( statistics.isStatisticsEnabled() ).isTrue();
	}

	@Test
	@SessionFactory()
	public void testMethodLevelAnnotations(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		assertThat( statistics.isStatisticsEnabled() ).isFalse();
	}
}
