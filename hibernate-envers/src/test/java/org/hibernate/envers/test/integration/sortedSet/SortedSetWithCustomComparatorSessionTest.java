package org.hibernate.envers.test.integration.sortedSet;

import org.hibernate.MappingException;
import org.hibernate.envers.test.AbstractOneSessionTest;
import org.hibernate.envers.test.Priority;
import org.hibernate.testing.FailureExpected;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.SortedSet;

import static org.junit.Assert.assertEquals;

/**
 * @author Michal Skowronek (mskowr at o2 pl)
 */
public class SortedSetWithCustomComparatorSessionTest extends AbstractOneSessionTest {

    private Integer id1;
    private Integer id2;
    private Integer id3;
    private Integer id4;

    protected void initMappings() throws MappingException, URISyntaxException {
        URL url = Thread.currentThread().getContextClassLoader().getResource("mappings/sortedSet/mappings.hbm.xml");
        config.addFile(new File(url.toURI()));
    }

    @Test
    @Priority(10)
    public void initData() {
        NotAnnotatedSortedSetEntity entity1 = new NotAnnotatedSortedSetEntity(1, "sortedSet1");

        // Revision 1
        getSession().getTransaction().begin();

        getSession().persist(entity1);

        getSession().getTransaction().commit();

        // Revision 2

        getSession().getTransaction().begin();

        entity1 = (NotAnnotatedSortedSetEntity) getSession().get(NotAnnotatedSortedSetEntity.class, 1);
        final NotAnnotatedStrTestEntity strTestEntity1 = new NotAnnotatedStrTestEntity("abc");
        getSession().persist(strTestEntity1);
        id1 = strTestEntity1.getId();
        entity1.getSortedSet().add(strTestEntity1);

        getSession().getTransaction().commit();

        // Revision 3
        getSession().getTransaction().begin();

        entity1 = (NotAnnotatedSortedSetEntity) getSession().get(NotAnnotatedSortedSetEntity.class, 1);
        final NotAnnotatedStrTestEntity strTestEntity2 = new NotAnnotatedStrTestEntity("aaa");
        getSession().persist(strTestEntity2);
        id2 = strTestEntity2.getId();
        entity1.getSortedSet().add(strTestEntity2);

        getSession().getTransaction().commit();

        // Revision 4
        getSession().getTransaction().begin();

        entity1 = (NotAnnotatedSortedSetEntity) getSession().get(NotAnnotatedSortedSetEntity.class, 1);
        final NotAnnotatedStrTestEntity strTestEntity3 = new NotAnnotatedStrTestEntity("aba");
        getSession().persist(strTestEntity3);
        id3 = strTestEntity3.getId();
        entity1.getSortedSet().add(strTestEntity3);

        getSession().getTransaction().commit();

        // Revision 5
        getSession().getTransaction().begin();

        entity1 = (NotAnnotatedSortedSetEntity) getSession().get(NotAnnotatedSortedSetEntity.class, 1);
        final NotAnnotatedStrTestEntity strTestEntity4 = new NotAnnotatedStrTestEntity("aac");
        getSession().persist(strTestEntity4);
        id4 = strTestEntity4.getId();
        entity1.getSortedSet().add(strTestEntity4);

        getSession().getTransaction().commit();
    }

    @Test
    public void testRevisionsCounts() {
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), getAuditReader().getRevisions(NotAnnotatedSortedSetEntity.class, 1));
        assertEquals(Arrays.asList(2), getAuditReader().getRevisions(NotAnnotatedStrTestEntity.class, id1));
        assertEquals(Arrays.asList(3), getAuditReader().getRevisions(NotAnnotatedStrTestEntity.class, id2));
        assertEquals(Arrays.asList(4), getAuditReader().getRevisions(NotAnnotatedStrTestEntity.class, id3));
        assertEquals(Arrays.asList(5), getAuditReader().getRevisions(NotAnnotatedStrTestEntity.class, id4));
    }

    @Test
    public void testCurrentStateOfEntity1() {
        final NotAnnotatedSortedSetEntity entity1 = (NotAnnotatedSortedSetEntity) getSession().get(
                NotAnnotatedSortedSetEntity.class, 1);

        assertEquals("sortedSet1", entity1.getData());
        assertEquals(Integer.valueOf(1), entity1.getId());

        final SortedSet<NotAnnotatedStrTestEntity> sortedSet = entity1.getSortedSet();
        assertEquals(NotAnnotatedStrTestEntityComparator.class, sortedSet.comparator().getClass());
        assertEquals(4, sortedSet.size());
        final Iterator<NotAnnotatedStrTestEntity> iterator = sortedSet.iterator();
        checkStrTestEntity(iterator.next(), id2, "aaa");
        checkStrTestEntity(iterator.next(), id4, "aac");
        checkStrTestEntity(iterator.next(), id3, "aba");
        checkStrTestEntity(iterator.next(), id1, "abc");
    }

    private void checkStrTestEntity(NotAnnotatedStrTestEntity entity, Integer id, String sortKey) {
        assertEquals(id, entity.getId());
        assertEquals(sortKey, entity.getStr());
    }

    @Test
    @FailureExpected(message = "Envers doesn't support custom comparators yet", jiraKey = "HHH-6176")
    public void testHistoryOfEntity1() throws Exception {
        NotAnnotatedSortedSetEntity entity1 = getAuditReader().find(NotAnnotatedSortedSetEntity.class, 1, 1);

        assertEquals("sortedSet1", entity1.getData());
        assertEquals(Integer.valueOf(1), entity1.getId());

        SortedSet<NotAnnotatedStrTestEntity> sortedSet = entity1.getSortedSet();
        assertEquals(NotAnnotatedStrTestEntityComparator.class, sortedSet.comparator().getClass());
        assertEquals(0, sortedSet.size());

        entity1 = getAuditReader().find(NotAnnotatedSortedSetEntity.class, 1, 2);

        assertEquals("sortedSet1", entity1.getData());
        assertEquals(Integer.valueOf(1), entity1.getId());

        sortedSet = entity1.getSortedSet();
        assertEquals(NotAnnotatedStrTestEntityComparator.class, sortedSet.comparator().getClass());
        assertEquals(1, sortedSet.size());
        Iterator<NotAnnotatedStrTestEntity> iterator = sortedSet.iterator();
        checkStrTestEntity(iterator.next(), id1, "abc");

        entity1 = getAuditReader().find(NotAnnotatedSortedSetEntity.class, 1, 3);

        assertEquals("sortedSet1", entity1.getData());
        assertEquals(Integer.valueOf(1), entity1.getId());

        sortedSet = entity1.getSortedSet();
        assertEquals(NotAnnotatedStrTestEntityComparator.class, sortedSet.comparator().getClass());
        assertEquals(2, sortedSet.size());
        iterator = sortedSet.iterator();
        checkStrTestEntity(iterator.next(), id2, "aaa");
        checkStrTestEntity(iterator.next(), id1, "abc");

        entity1 = getAuditReader().find(NotAnnotatedSortedSetEntity.class, 1, 4);

        assertEquals("sortedSet1", entity1.getData());
        assertEquals(Integer.valueOf(1), entity1.getId());

        sortedSet = entity1.getSortedSet();
        assertEquals(NotAnnotatedStrTestEntityComparator.class, sortedSet.comparator().getClass());
        assertEquals(3, sortedSet.size());
        iterator = sortedSet.iterator();
        checkStrTestEntity(iterator.next(), id2, "aaa");
        checkStrTestEntity(iterator.next(), id3, "aba");
        checkStrTestEntity(iterator.next(), id1, "abc");

        entity1 = getAuditReader().find(NotAnnotatedSortedSetEntity.class, 1, 5);

        assertEquals("sortedSet1", entity1.getData());
        assertEquals(Integer.valueOf(1), entity1.getId());

        sortedSet = entity1.getSortedSet();
        assertEquals(NotAnnotatedStrTestEntityComparator.class, sortedSet.comparator().getClass());
        assertEquals(4, sortedSet.size());
        iterator = sortedSet.iterator();
        checkStrTestEntity(iterator.next(), id2, "aaa");
        checkStrTestEntity(iterator.next(), id4, "aac");
        checkStrTestEntity(iterator.next(), id3, "aba");
        checkStrTestEntity(iterator.next(), id1, "abc");
    }

}
