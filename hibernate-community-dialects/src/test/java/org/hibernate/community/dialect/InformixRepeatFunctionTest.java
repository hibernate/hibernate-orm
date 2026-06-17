/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.util.ast.HqlHelper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel
@SessionFactory
@ServiceRegistry(
		settings = @Setting( name = AvailableSettings.DIALECT, value = "org.hibernate.community.dialect.InformixDialect" )
)
class InformixRepeatFunctionTest {

	@Test
	@JiraKey( "HHH-20306" )
	void repeatRendersWithSpaceReplaceEmulation(SessionFactoryScope scope) {
		final HqlHelper.HqlTranslation translation = HqlHelper.translateHql(
				"select repeat('hello', 3)",
				String.class,
				scope.getSessionFactory()
		);

		assertThat( translation.sql() ).isEqualTo(
				"select replace(space(3),' ','hello') from (select 0 from systables where tabid=1) dual"
		);
	}
}
