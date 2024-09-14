/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.naturalid.cid;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.Setting;

import static org.hibernate.cfg.AvailableSettings.GENERATE_STATISTICS;
import static org.hibernate.cfg.AvailableSettings.USE_QUERY_CACHE;
import static org.hibernate.cfg.AvailableSettings.USE_SECOND_LEVEL_CACHE;

/**
 * @author Donnchadh O Donnabhain
 */
@ServiceRegistry(
        settings = {
                @Setting( name = USE_SECOND_LEVEL_CACHE, value = "false" ),
                @Setting( name = USE_QUERY_CACHE, value = "false" ),
                @Setting( name = GENERATE_STATISTICS, value = "false" )
        }
)
@DomainModel( xmlMappings = "org/hibernate/orm/test/mapping/naturalid/cid/Account.hbm.xml" )
@SessionFactory
public class HbmCompositeIdAndNaturalIdTest extends AbstractCompositeIdAndNaturalIdTest {
}
