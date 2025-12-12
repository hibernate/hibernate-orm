package org.hibernate.orm.test.joinformulamappedby;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.junit.Test;
import org.hibernate.testing.TestForIssue;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
* @author vladimir.martinek
 */
public class JoinFormulaMappedByTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	public Class[] getAnnotatedClasses() {
	    return new Class[] { TESTLink.class, TESTNode.class, TESTAttribute.class };
	}

	@Test
	@TestForIssue(jiraKey = "HHH-16922")
	public void test() {
		doInJPA( this::entityManagerFactory, em -> {

				 }
		);
	}
    /*
	@Test
	public void test() throws Exception
	{
		DBFactory dbf = TestSuite.getDBFactory();
		TESTDAO tdao = ApplicationContextProvider.getBean(TESTDAO.class);
		TESTNode tn = tdao.createStructure();
		//tn = dbf.find(TESTNode.class, tn.getId(), false, "nodeWithCAs");
		tn = dbf.find(TESTNode.class, tn.getId(), false);
		//System.out.println(tn.getCas().size());
		TESTAttributePk id = new TESTAttributePk(TESTNode.class, tn.getId(), "ATTR1");
		TESTAttribute ca = dbf.find(TESTAttribute.class, id, false, "attrWithNode", "attrWithLink");
		System.out.println(ca.getAttrName());
		System.out.println(ca.getEntityId());
		System.out.println(ca.getEntityClass());
		System.out.println(ca.getNode());
		System.out.println(ca.getLink());
		
	}
    */
}
