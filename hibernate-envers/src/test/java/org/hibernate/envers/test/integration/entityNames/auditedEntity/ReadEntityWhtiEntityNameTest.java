package org.hibernate.envers.test.integration.entityNames.auditedEntity;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.envers.test.AbstractSessionTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author Hern&aacute;n Chanfreau
 * 
 */

@Test(sequential=true)
public class ReadEntityWhtiEntityNameTest extends AbstractSessionTest{

	private long id_pers1;
	private long id_pers2;
	private long id_pers3;
	
	private Person person1_1;
	private Person person1_2;
	private Person person1_3;
	
	private Person currentPers1;
	
	protected void initMappings() throws MappingException, URISyntaxException {
		URL url = Thread.currentThread().getContextClassLoader().getResource("mappings/entityNames/auditedEntity/mappings.hbm.xml");
        config.addFile(new File(url.toURI()));
	}
	
	/**
	 * The test needs to run with the same session and auditReader.
	 */
	@Override
	public void newSessionFactory() {
		if (getSession() == null) {
			super.newSessionFactory();
		}
	}
	
    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
    	
    	newSessionFactory();

        Person pers1 = new Person("Hernan", 28);
        Person pers2 = new Person("Leandro", 29);
        Person pers3 = new Person("Barba", 30);
        
        //REV 1 
        getSession().getTransaction().begin();
        getSession().persist("Personaje",pers1);
        id_pers1 = pers1.getId();
        getSession().getTransaction().commit();

        //REV 2
        getSession().getTransaction().begin();
        pers1 = (Person)getSession().get("Personaje", id_pers1);
        pers1.setAge(29);
        getSession().persist("Personaje",pers1);
        getSession().persist("Personaje",pers2);
        id_pers2 = pers2.getId();
        getSession().getTransaction().commit();
        
        //REV 
        getSession().getTransaction().begin();
        pers1 = (Person)getSession().get("Personaje", id_pers1);
        pers1.setName("Hernan David");
        pers2 = (Person)getSession().get("Personaje", id_pers2);
        pers2.setAge(30);
        getSession().persist("Personaje",pers1);
        getSession().persist("Personaje",pers2);
        getSession().persist("Personaje",pers3);
        id_pers3 = pers3.getId();
        getSession().getTransaction().commit();
        
        getSession().getTransaction().begin();
        currentPers1 = (Person)getSession().get("Personaje", id_pers1);
        getSession().getTransaction().commit();
        
    }
    
    
    @Test
    public void testRetrieveRevisionsWithEntityName() {
    	List<Number> pers1Revs = getAuditReader().getRevisions(Person.class,"Personaje", id_pers1);
    	List<Number> pers2Revs = getAuditReader().getRevisions(Person.class,"Personaje", id_pers2);
    	List<Number> pers3Revs = getAuditReader().getRevisions(Person.class,"Personaje", id_pers3);
    	
    	assert(pers1Revs.size() == 3);
    	assert(pers2Revs.size() == 2);
    	assert(pers3Revs.size() == 1);
    }
    
    @Test(dependsOnMethods="testRetrieveRevisionsWithEntityName")
    public void testRetrieveAuditedEntityWithEntityName() {
    	person1_1 = getAuditReader().find(Person.class, "Personaje", id_pers1, 1);
    	person1_2 = getAuditReader().find(Person.class, "Personaje", id_pers1, 2);
    	person1_3 = getAuditReader().find(Person.class, "Personaje", id_pers1, 3);
    	
    	person1_1.getName();
    	person1_1.getAge();
    	person1_2.getName();
    	person1_2.getAge();
    	person1_3.getName();
    	person1_3.getAge();
    }
    
    @Test(dependsOnMethods="testRetrieveAuditedEntityWithEntityName")
    public void testObtainEntityNameAuditedEntityWithEntityName() {
    	
    	String currentPers1EN = getSession().getEntityName(currentPers1);
    	
    	String person1EN = getAuditReader().getEntityName(person1_1.getId(), 1, person1_1);
    	assert (currentPers1EN.equals(person1EN)); 

    	String person2EN = getAuditReader().getEntityName(person1_2.getId(), 2, person1_2);
    	assert (currentPers1EN.equals(person2EN)); 

    	String person3EN = getAuditReader().getEntityName(person1_3.getId(), 3, person1_3);
    	assert (currentPers1EN.equals(person3EN)); 

    }    
    
    @Test(dependsOnMethods="testObtainEntityNameAuditedEntityWithEntityName")
    public void testFindHistoricAndCurrentForAuditedEntityWithEntityName() {
    	
    	// force a new session and AR
    	super.newSessionFactory();

    	person1_1 = getAuditReader().find(Person.class, "Personaje", id_pers1, 1);
    	person1_2 = getAuditReader().find(Person.class, "Personaje", id_pers1, 2);
    	person1_3 = getAuditReader().find(Person.class, "Personaje", id_pers1, 3);
    	
    	person1_1.getName();
    	person1_1.getAge();
    	person1_2.getName();
    	person1_2.getAge();
    	person1_3.getName();
    	person1_3.getAge();    	
    }    
    
    
}
