/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.type.wrapperoptions;

import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.NonContextualLobCreator;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.type.descriptor.WrapperOptions;

import java.sql.Types;
import java.util.TimeZone;

/**
 * WrapperOptionsmock  implementation to use with all JavaType wrapper classes.
 */
@JiraKey("HHH-17507")
@JiraKey("HHH-17574")
public class MockWrapperOptions implements WrapperOptions {

    private final boolean useStreamForLobBinding;

    public MockWrapperOptions(final boolean useStreamForLobBinding) {
        this.useStreamForLobBinding = useStreamForLobBinding;
    }

    @Override
    public SharedSessionContractImplementor getSession() {
        return null;
    }

    @Override
    public SessionFactoryImplementor getSessionFactory() {
        return null;
    }

    @Override
    public boolean useStreamForLobBinding() {
        return useStreamForLobBinding;
    }

    @Override
    public int getPreferredSqlTypeCodeForBoolean() {
        return Types.BOOLEAN;
    }

    @Override
    public LobCreator getLobCreator() {
        return NonContextualLobCreator.INSTANCE;
    }

    @Override
    public TimeZone getJdbcTimeZone() {
        return null;
    }

}
