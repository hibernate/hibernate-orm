package org.hibernate.envers.test.integration.readwriteexpression;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.Priority;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.util.List;

public class ReadWriteExpressionChange extends AbstractEntityTest {

    private static final double HEIGHT_INCHES = 73;
    private static final double HEIGHT_CENTIMETERS = HEIGHT_INCHES * 2.54d;

    private Integer id;

    @Override
    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(Staff.class);
    }

    @Test
    @Priority(10)
    public void initData() {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        Staff staff = new Staff(HEIGHT_INCHES, 1);
        em.persist(staff);
        em.getTransaction().commit();
        id = staff.getId();
    }

    @Test
    public void shouldRespectWriteExpression() {
        EntityManager em = getEntityManager();
        List resultList = em.createNativeQuery("select size_in_cm from t_staff_AUD where id ="+id).getResultList();
        assert 1 == resultList.size();
        Double sizeInCm = (Double) resultList.get(0);
        assert sizeInCm.equals(HEIGHT_CENTIMETERS);
    }

    @Test
    public void shouldRespectReadExpression() {
        List<Number> revisions = getAuditReader().getRevisions(Staff.class, id);
        assert 1 == revisions.size();
        Number number = revisions.get(0);
        Staff staffRev = getAuditReader().find(Staff.class, id, number);
        assert HEIGHT_INCHES == staffRev.getSizeInInches();
    }

}
