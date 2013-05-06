package org.hibernate.envers.test.integration.readwriteexpression;

import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;

import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;

import org.junit.Assert;
import org.junit.Test;

public class ReadWriteExpressionChange extends BaseEnversJPAFunctionalTestCase {

	private static final Double HEIGHT_INCHES = 73.0d;
	private static final Double HEIGHT_CENTIMETERS = HEIGHT_INCHES * 2.54d;

	private Integer id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Staff.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		Staff staff = new Staff( HEIGHT_INCHES, 1 );
		em.persist( staff );
		em.getTransaction().commit();
		id = staff.getId();
	}

	@Test
	public void shouldRespectWriteExpression() {
		EntityManager em = getEntityManager();
		List resultList = em.createNativeQuery( "select size_in_cm from t_staff_AUD where id =" + id ).getResultList();
		Assert.assertEquals( 1, resultList.size() );
		Double sizeInCm = null;
		if ( getDialect() instanceof Oracle8iDialect ) {
			sizeInCm = ((BigDecimal) resultList.get( 0 )).doubleValue();
		}
		else {
			sizeInCm = (Double) resultList.get( 0 );
		}
		Assert.assertEquals( HEIGHT_CENTIMETERS, sizeInCm.doubleValue(), 0.00000001 );
	}

	@Test
	public void shouldRespectReadExpression() {
		List<Number> revisions = getAuditReader().getRevisions( Staff.class, id );
		Assert.assertEquals( 1, revisions.size() );
		Number number = revisions.get( 0 );
		Staff staffRev = getAuditReader().find( Staff.class, id, number );
		Assert.assertEquals( HEIGHT_INCHES, staffRev.getSizeInInches(), 0.00000001 );
	}

}
