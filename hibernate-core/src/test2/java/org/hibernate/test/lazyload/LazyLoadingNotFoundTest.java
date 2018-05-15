/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.lazyload;

import org.hibernate.LazyInitializationException;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author Nikolay Golubev
 */
public class LazyLoadingNotFoundTest extends BaseCoreFunctionalTestCase {

    protected void configure(Configuration cfg) {
        super.configure(cfg);
        cfg.setProperty(Environment.ENABLE_LAZY_LOAD_NO_TRANS, "true");
    }

    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{
                Parent.class,
                Child.class
        };
    }

    @Test
    @TestForIssue(jiraKey = "HHH-11179")
    public void testNonExistentLazyInitOutsideTransaction() {
        Session s = openSession();
        s.beginTransaction();
        Child loadedChild = s.load(Child.class, -1L);
        s.getTransaction().commit();
        s.close();

        try {
            loadedChild.getParent();
            fail("lazy init did not fail on non-existent proxy");
        } catch (LazyInitializationException e) {
            assertNotNull(e.getMessage());
        }
    }
}
