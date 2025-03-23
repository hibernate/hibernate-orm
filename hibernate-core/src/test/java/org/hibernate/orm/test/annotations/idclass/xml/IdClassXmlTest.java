/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.idclass.xml;


import java.util.List;

import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A test for HHH-4282
 *
 * @author Hardy Ferentschik
 */
//@FailureExpected( jiraKey = "HHH-4282" )
@DomainModel(
		annotatedClasses = HabitatSpeciesLink.class,
		xmlMappings = "org/hibernate/orm/test/annotations/idclass/xml/HabitatSpeciesLink.xml"
)
@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "'link' is not escaped even though autoQuoteKeywords is enabled" )
@SessionFactory
public class IdClassXmlTest {
	@Test
	public void testEntityMappingPropertiesAreNotIgnored(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					HabitatSpeciesLink link = new HabitatSpeciesLink();
					link.setHabitatId( 1L );
					link.setSpeciesId( 1L );
					session.persist( link );

					Query q = session.getNamedQuery( "testQuery" );
					final List<HabitatSpeciesLink> list = q.list();
					assertEquals( 1, list.size() );
				}
		);
	}
}
