/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.access;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
public class XmlDefaultAccessTypeTest {
	@Test
	@DomainModel(xmlMappings = "mappings/access/default-complete.xml")
	@SessionFactory
	void testCompleteMapping(SessionFactoryScope factoryScope) {
		var entityDescriptor = factoryScope.getSessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor( XmlMappedEntity.class );

		var nameAttribute = entityDescriptor.findAttributeMapping( "name" );
		assertThat( nameAttribute.getPropertyAccess().getGetter().getMember() ).isInstanceOf( Method.class );
	}

	@Test
	@DomainModel(xmlMappings = "mappings/access/default-incomplete.xml")
	@SessionFactory
	void testIncompleteMapping(SessionFactoryScope factoryScope) {
		var entityDescriptor = factoryScope.getSessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor( XmlMappedEntity.class );

		var nameAttribute = entityDescriptor.findAttributeMapping( "name" );
		assertThat( nameAttribute.getPropertyAccess().getGetter().getMember() ).isInstanceOf( Method.class );
	}
}
