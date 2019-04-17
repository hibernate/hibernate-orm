/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm.execution;

import org.hibernate.boot.MetadataSources;
import org.hibernate.testing.orm.domain.StandardDomainModel;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Test;

/**
 * @author Gavin King
 */
public class FunctionTests extends SessionFactoryBasedFunctionalTest {

    @Override
    protected void applyMetadataSources(MetadataSources metadataSources) {
        StandardDomainModel.GAMBIT.getDescriptor().applyDomainModel(metadataSources);
    }

    @Test
    public void testCastFunction() {
        inTransaction(
                session -> {
                    session.createQuery("select cast(e.theDate as string) from EntityOfBasics e")
                            .list();
                    session.createQuery("select cast(e.id as string) from EntityOfBasics e")
                            .list();
                }
        );
    }

    @Test
    public void testExtractFunction() {
        inTransaction(
                session -> {
                    session.createQuery("select extract(year from e.theDate) from EntityOfBasics e")
                            .list();
                    session.createQuery("select extract(month from e.theDate) from EntityOfBasics e")
                            .list();
                    session.createQuery("select extract(day from e.theDate) from EntityOfBasics e")
                            .list();

                    session.createQuery("select extract(hour from e.theTime) from EntityOfBasics e")
                            .list();
                    session.createQuery("select extract(minute from e.theTime) from EntityOfBasics e")
                            .list();
                    session.createQuery("select extract(second from e.theTime) from EntityOfBasics e")
                            .list();

                    session.createQuery("select extract(year from e.theTimestamp) from EntityOfBasics e")
                            .list();
                    session.createQuery("select extract(month from e.theTimestamp) from EntityOfBasics e")
                            .list();
                    session.createQuery("select extract(day from e.theTimestamp) from EntityOfBasics e")
                            .list();
                    session.createQuery("select extract(hour from e.theTimestamp) from EntityOfBasics e")
                            .list();
                    session.createQuery("select extract(minute from e.theTimestamp) from EntityOfBasics e")
                            .list();
                    session.createQuery("select extract(second from e.theTimestamp) from EntityOfBasics e")
                            .list();
                }
        );
    }

}