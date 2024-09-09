/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.internal.util;

import org.hibernate.FlushMode;
import org.hibernate.Session;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael Spahn
 */
@JiraKey(value = "HHH-13974")
@DomainModel
@SessionFactory
public class SessionBuilderFlushModeTest {

	@ParameterizedTest
	@EnumSource( FlushMode.class )
	public void testFlushModeSettingTakingEffect(FlushMode flushMode, SessionFactoryScope scope) {
		try ( final Session session = scope.getSessionFactory().withOptions().flushMode( flushMode ).openSession() ) {
			assertThat( session.getHibernateFlushMode() ).isEqualTo( flushMode );
		}
	}
}
