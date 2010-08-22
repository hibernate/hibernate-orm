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
 * @author Hern�n Chanfreau
 * 
 */

public class ReadEntityWhtiEntityNameTest extends AbstractSessionTest{

	private long id_pers1;
	private long id_pers2;
	private long id_pers3;
	
	
	protected void initMappings() throws MappingException, URISyntaxException {
		URL url = Thread.currentThread().getContextClassLoader().getResource("mappings/entityNames/auditedEntity/mappings.hbm.xml");
        config.addFile(new File(url.toURI()));
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
    
    @Test
    public void testRetrieveAuditedEntityWithEntityName() {
    	Person Person1 = getAuditReader().find(Person.class, "Personaje", id_pers1, 1);
    	Person Person2 = getAuditReader().find(Person.class, "Personaje", id_pers1, 2);
    	Person Person3 = getAuditReader().find(Person.class, "Personaje", id_pers1, 3);
    	
    	System.out.println("Revision 1:");
    	System.out.println("  > Name: " + Person1.getName());
    	System.out.println("  > Age: " + Person1.getAge());
    	System.out.println("Revision 2:");
    	System.out.println("  > Name: " + Person2.getName());
    	System.out.println("  > Age: " + Person2.getAge());
    	System.out.println("Revision 3:");
    	System.out.println("  > Name: " + Person3.getName());
    	System.out.println("  > Age: " + Person3.getAge());
    }
    

}
