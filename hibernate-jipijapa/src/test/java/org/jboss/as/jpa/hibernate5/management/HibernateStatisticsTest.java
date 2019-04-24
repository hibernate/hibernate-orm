/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.jboss.as.jpa.hibernate5.management;

import org.junit.Test;

import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

public class HibernateStatisticsTest
{

    @Test
    public void canInitialiseStatistics() {
        HibernateStatistics statistics = new HibernateStatistics();

        assertThat(statistics.getTypes().size(), not(0));
        assertThat(statistics.getChildrenNames().size(), not(0));
        assertThat(statistics.getChildrenNames().size(), not(0));
    }

}
