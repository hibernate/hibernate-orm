package org.hibernate.envers.test.integration.entityNames.singleAssociatedAudited;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.hibernate.MappingException;
import org.hibernate.envers.test.AbstractSessionTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author Hern�n Chanfreau
 * 
 */

public class ReadEntityAssociatedAuditedTest extends AbstractSessionTest{

	private long id_car1;
	private long id_car2;
	
	private long id_pers1; 
	private long id_pers2; 
	
	
	protected void initMappings() throws MappingException, URISyntaxException {
		URL url = Thread.currentThread().getContextClassLoader().getResource("mappings/entityNames/singleAssociatedAudited/mappings.hbm.xml");
        config.addFile(new File(url.toURI()));
	}
	
	
    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
    	
    	newSessionFactory();

        Person pers1 = new Person("Hernan", 15);
        Person pers2 = new Person("Leandro", 19);
        
        Car car1 = new Car(1, pers1);
        Car car2 = new Car(2, pers2);
        
        //REV 1 
        getSession().getTransaction().begin();
        getSession().persist("Personaje",pers1);
        getSession().persist(car1);
        getSession().getTransaction().commit();
        id_car1 = car1.getId();
        id_pers1 = pers1.getId();

        //REV 2
        getSession().getTransaction().begin();
        pers1.setAge(50);
        getSession().persist("Personaje", pers1);
        getSession().persist("Personaje", pers2);
        getSession().persist(car2);
        getSession().getTransaction().commit();
        id_car2 = car2.getId();
        id_pers2 = pers2.getId();

    }
    
    @Test
    public void testGetAssociationWithEntityName() {

    	Person person1 = (Person)getSession().get("Personaje", id_pers1);
    	Car car1 = getAuditReader().find(Car.class, id_car1, 1);
    	Person person1_1 = car1.getOwner();
    	assert(person1.getAge() != person1_1.getAge());
    	
    	Person person2 = (Person)getSession().get("Personaje", id_pers2);
    	Car car2 = getAuditReader().find(Car.class, id_car2, 2);
    	Person person2_1 = car2.getOwner();
    	assert(person2.getAge() == person2_1.getAge());
    }

}
