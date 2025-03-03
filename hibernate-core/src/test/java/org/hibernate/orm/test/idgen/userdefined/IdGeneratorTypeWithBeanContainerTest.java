/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.userdefined;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Yanming Zhou
 */
@JiraKey( "HHH-18164" )
@SessionFactory
@ServiceRegistry(
		settings = {
			@Setting(name = AvailableSettings.ALLOW_EXTENSIONS_IN_CDI, value = "true"),
			@Setting(name = AvailableSettings.BEAN_CONTAINER, value = "org.hibernate.orm.test.idgen.userdefined.SimpleBeanContainer")
		}
)
@DomainModel(annotatedClasses = IdGeneratorTypeWithBeanContainerTest.SimpleEntity.class)
public class IdGeneratorTypeWithBeanContainerTest {

	@Test void test(SessionFactoryScope scope) {
		SimpleEntity entity = new SimpleEntity();
		scope.inTransaction(s -> s.persist(entity));
		assertThat(entity.id, is(SimpleBeanContainer.INITIAL_VALUE));
	}

	@Entity
	public static class SimpleEntity {
		@Id @SimpleId
		long id;
		String data;
	}

}
