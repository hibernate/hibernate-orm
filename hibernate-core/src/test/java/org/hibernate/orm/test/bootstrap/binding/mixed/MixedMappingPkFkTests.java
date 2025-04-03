/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.mixed;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.Jira;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Russ Tennant (russ@venturetech.net)
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@Jira( "https://hibernate.atlassian.net/browse/HHH-11502" )
@DomainModel(
		annotatedClasses = AnnotationEntity.class,
		xmlMappings = "/org/hibernate/orm/test/bootstrap/binding/mixed/XmlEntity.xml"
)
public class MixedMappingPkFkTests {
	@Test
	void testMapping(DomainModelScope modelScope) {
		// just trigger the build
		assertThat( modelScope.getDomainModel() ).isNotNull();
	}
}
