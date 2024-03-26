/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.hbm.query;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.named.NamedObjectRepository;
import org.hibernate.query.sqm.spi.NamedSqmQueryMemento;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
public class XmlCacheableNamedQueryTests {
	@Test
	@DomainModel(
			annotatedClasses = SimpleEntity.class,
			xmlMappings = "org/hibernate/orm/test/hbm/query/CacheableNamedQueryOverride.xml"
	)
	@SessionFactory
	@SuppressWarnings("JUnitMalformedDeclaration")
	void testCacheableQueryOverride(SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final NamedObjectRepository namedObjectRepository = sessionFactory.getQueryEngine().getNamedObjectRepository();
		final NamedSqmQueryMemento queryMemento = namedObjectRepository.getSqmQueryMemento( SimpleEntity.FIND_ALL );
		assertThat( queryMemento ).isNotNull();
		assertThat( queryMemento.getCacheable() ).isNotNull();
		assertThat( queryMemento.getCacheable() ).isTrue();
	}
}
