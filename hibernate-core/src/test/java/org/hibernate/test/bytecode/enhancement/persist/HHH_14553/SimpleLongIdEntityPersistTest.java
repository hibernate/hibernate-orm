/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.persist.HHH_14553;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Karsten Wutzke
 */
@TestForIssue( jiraKey = "HHH-14553")
@RunWith( BytecodeEnhancerRunner.class ) // <-- without this, persist works
public class SimpleLongIdEntityPersistTest extends BaseEntityManagerFunctionalTestCase {

    @Override
    public Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{Ship.class};
    }

    @Override
    protected void addConfigOptions(Map options) {
        options.put( AvailableSettings.USE_SECOND_LEVEL_CACHE, "false" );
        options.put( AvailableSettings.SHOW_SQL, "true" );
        options.put( AvailableSettings.FORMAT_SQL, "true" );
    }

    @Before
    public void prepare() {
        doInJPA( this::entityManagerFactory, em -> {
            Ship entity = new Ship(1L, "Titanic");
            em.persist( entity );
        });
    }

    /**
     * Use --debug to see the SQL being run + the sysouts.
     */
    @Test
    public void test() {
        doInJPA( this::entityManagerFactory, em -> {
            Ship entity = em.find( Ship.class, 1L );

            Assert.assertNotNull("Ship not found!", entity);
        } );
    }
}
