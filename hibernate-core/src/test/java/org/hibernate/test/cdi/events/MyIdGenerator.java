/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.cdi.events;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;

/**
 * A custom ID generator that does not require CDI, so should be instantiated correctly in every scenario.
 * <p>
 * Not really functional: only ever generates a single hardcoded ID, for convenience of testing.
 */
public class MyIdGenerator implements IdentifierGenerator {
    public static final int HARDCODED_ID = 42;

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
        return HARDCODED_ID;
    }
}
