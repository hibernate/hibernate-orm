package org.hibernate.envers.test.integration.reventity.trackmodifiedentities;

import org.hibernate.MappingException;
import org.hibernate.envers.test.AbstractSessionTest;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.integration.entityNames.manyToManyAudited.Car;
import org.hibernate.envers.test.integration.entityNames.manyToManyAudited.Person;
import org.hibernate.envers.test.tools.TestTools;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class EntityNamesTest extends AbstractSessionTest {
	protected void initMappings() throws MappingException, URISyntaxException {
		URL url = Thread.currentThread().getContextClassLoader().getResource("mappings/entityNames/manyToManyAudited/mappings.hbm.xml");
        config.addFile(new File(url.toURI()));
        config.setProperty("org.hibernate.envers.track_entities_changed_in_revision", "true");
	}

    @Test
    @Priority(10)
    public void initData() {
        Person pers1 = new Person("Hernan", 28);
        Person pers2 = new Person("Leandro", 29);
        Person pers3 = new Person("Barba", 32);
        Person pers4 = new Person("Camomo", 15);

        // Revision 1
        getSession().getTransaction().begin();
        List<Person > owners = new ArrayList<Person>();
        owners.add(pers1);
        owners.add(pers2);
        owners.add(pers3);
        Car car1 = new Car(5, owners);
        getSession().persist(car1);
        getSession().getTransaction().commit();
        long person1Id = pers1.getId();

        // Revision 2
        owners = new ArrayList<Person>();
        owners.add(pers2);
        owners.add(pers3);
        owners.add(pers4);
        Car car2 = new Car(27, owners);
        getSession().getTransaction().begin();
        Person person1 = (Person)getSession().get("Personaje", person1Id);
        person1.setName("Hernan David");
        person1.setAge(40);
        getSession().persist(car1);
        getSession().persist(car2);
        getSession().getTransaction().commit();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testModifiedEntityTypes() {
        assert TestTools.makeSet(Car.class, Person.class).equals(getAuditReader().findEntityTypesChangedInRevision(1));
        assert TestTools.makeSet(Car.class, Person.class).equals(getAuditReader().findEntityTypesChangedInRevision(2));
    }
}