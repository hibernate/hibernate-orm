package org.hibernate.bytecode.internal.bytebuddy;

import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.*;
import java.util.Map;

/**
 *
 */
@RunWith(BytecodeEnhancerRunner.class)
@TestForIssue( jiraKey="HHH-13129" )
public class CascadeOnDetachedLazyTest extends BaseNonConfigCoreFunctionalTestCase
{
    @Override
    protected Class<?>[] getAnnotatedClasses()
    {
        return new Class<?>[]{
                Person.class,
                Address.class,
        };
    }

    @Override
    protected void addSettings(Map settings)
    {
        super.addSettings(settings);
        settings.put(AvailableSettings.SHOW_SQL, "true");
        settings.put(AvailableSettings.FORMAT_SQL, "true");
    }

    @Test
    public void testMergeDetachedEnhancedEntityWithUninitializedLazyToOne()
    {
        Person originalPerson = persistPerson();

        Person loadedPerson = get(originalPerson.getId());
        loadedPerson.setName("newName");

        Person mergedPerson = merge(loadedPerson);
    }

    public void persist(Person p)
    {
        inTransaction(s ->
        {
            s.persist(p);
        });
    }

    public Person get(Long id)
    {
        Person[] result = new Person[1];
        inTransaction(s ->
        {
            result[0] = s.get(Person.class, id);
        });
        return result[0];
    }

    public Person merge(Person p)
    {
        Person[] result = new Person[1];
        inTransaction(s ->
        {
            result[0] = (Person) s.merge(p);
        });
        return result[0];
    }


    private Person persistPerson()
    {
        Address address = new Address();
        address.setDescription("ABC");

        Person person = new Person();
        person.setName("John Doe");
        person.setAddress(address);

        persist(person);

        return person;
    }

    @Entity
    @Table(name = "TEST_PERSON")
    public static class Person
    {
        @Id
        @GeneratedValue
        private Long id;

        @Column(name = "NAME", length = 300, nullable = true)
        private String name;

        @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
        @JoinColumn(name = "ADDRESS_ID")
        @LazyToOne(LazyToOneOption.NO_PROXY)
        private Address address;

        public Long getId()
        {
            return id;
        }

        public void setId(Long id)
        {
            this.id = id;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public Address getAddress()
        {
            return address;
        }

        public void setAddress(Address address)
        {
            this.address = address;
        }
    }

    @Entity
    @Table(name = "TEST_ADDRESS")
    public static class Address
    {
        @Id
        @GeneratedValue
        private Long id;

        @Column(name = "DESCRIPTION", length = 300, nullable = true)
        private String description;

        public Long getId()
        {
            return id;
        }

        public void setId(Long id)
        {
            this.id = id;
        }

        public String getDescription()
        {
            return description;
        }

        public void setDescription(String description)
        {
            this.description = description;
        }
    }
}


