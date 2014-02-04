package org.hibernate.action;

import junit.framework.TestCase;
import org.hibernate.HibernateException;

import java.io.Serializable;

/**
 * Tests relating to {@link org.hibernate.action.ExecutableList} instances.
 *
 * @author Steve Ebersole
 */
public class ExecutableListTest extends TestCase {

    public void testExecutableList() {
        ExecutableList list = new ExecutableList();
        assertTrue(list.getPropertySpaces().isEmpty());
        Executable executable = new Executable() {

            public Serializable[] getPropertySpaces() {
                return new Serializable[]{"space"};
            }

            public void beforeExecutions() throws HibernateException {
            }

            public void execute() throws HibernateException {
            }

            public AfterTransactionCompletionProcess getAfterTransactionCompletionProcess() {
                return null;
            }

            public BeforeTransactionCompletionProcess getBeforeTransactionCompletionProcess() {
                return null;
            }
        };
        list.add(executable);
        assertEquals(1, list.size());
        assertEquals(1, list.getPropertySpaces().size());
        assertTrue(list.getPropertySpaces().contains("space"));
        list.remove(executable);
        assertEquals(0, list.size());
        assertTrue(list.getPropertySpaces().isEmpty());
        list.add(executable);
        assertEquals(1, list.size());
        assertEquals(1, list.getPropertySpaces().size());
        assertTrue(list.getPropertySpaces().contains("space"));
        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.getPropertySpaces().isEmpty());
    }
}
