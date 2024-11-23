package org.hibernate.persister.entity;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinColumnsOrFormulas;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Test;

@TestForIssue(jiraKey = "HHH-14223")
public class JoinFormulaImplicitJoinTest extends BaseEntityManagerFunctionalTestCase {
    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] {
            Person.class, PersonVersion.class
        };
    }

    @Override
    protected void addConfigOptions(Map options) {
        options.put(
            AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS,
            Boolean.TRUE
        );
    }

    @Before
    public void setUp() {
        doInJPA( this::entityManagerFactory, entityManager -> {
            final Person person = new Person();
            entityManager.persist(person);

            for (int i = 0; i < 3; i++) {
                final PersonVersion personVersion = new PersonVersion();
                personVersion.setName("Name" + i);
                personVersion.setVersion(i);
                personVersion.setPerson(person);
                entityManager.persist(personVersion);
            }
        });
    }

    protected int entityCount() {
        return 5;
    }

    @Test
    public void testImplicitJoin() {
        doInJPA( this::entityManagerFactory, entityManager -> {
            entityManager.createQuery(
                "SELECT person\n" +
                    "FROM Person AS person\n" +
                    "    LEFT JOIN FETCH person.latestPersonVersion\n" +
                    "order by person.latestPersonVersion.id desc\n"
            );
        });
    }

    @Entity(name = "Person")
    public static class Person {

        @Id
        @GeneratedValue
        private Long id;

        @OneToMany(mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<PersonVersion> personVersions;

        @ManyToOne
        @JoinColumnsOrFormulas({
            @JoinColumnOrFormula(
                formula = @JoinFormula(
                    value = "(SELECT person_version.id FROM person_version WHERE person_version.person_id = id ORDER BY person_version.version DESC LIMIT 1)",
                    referencedColumnName = "id")
            )
        })
        private PersonVersion latestPersonVersion;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public List<PersonVersion> getPersonVersions() {
            return personVersions;
        }

        public void setPersonVersions(List<PersonVersion> personVersions) {
            this.personVersions = personVersions;
        }

        public PersonVersion getLatestPersonVersion() {
            return latestPersonVersion;
        }

        public void setLatestPersonVersion(PersonVersion latestPersonVersion) {
            this.latestPersonVersion = latestPersonVersion;
        }
    }

    @Entity(name = "PersonVersion")
    public static class PersonVersion {

        @Id
        @GeneratedValue
        private Long id;

        private String name;

        private Integer version;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "person_id")
        private Person person;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getVersion() {
            return version;
        }

        public void setVersion(Integer version) {
            this.version = version;
        }

        public Person getPerson() {
            return person;
        }

        public void setPerson(Person person) {
            this.person = person;
        }
    }
}
