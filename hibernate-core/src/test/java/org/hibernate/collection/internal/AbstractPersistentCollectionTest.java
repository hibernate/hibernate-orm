package org.hibernate.collection.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.internal.build.AllowSysOut;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

public class AbstractPersistentCollectionTest extends BaseCoreFunctionalTestCase {

    public static final int NUMBER_OF_THREADS = 3;
    public static final int NUMBER_OF_ENTRIES = 10;
    private College college;

    private College loadedCollege;

    private final ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    @Override
    protected void configure(Configuration configuration) {
        configuration.setProperty( AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, Boolean.TRUE.toString() );
        configuration.setProperty( AvailableSettings.POOL_SIZE, String.valueOf(10) );

    }

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] {
                College.class,
                Student.class
        };
    }

    @Override
    protected void prepareTest() throws Exception {
        doInHibernate( this::sessionFactory, session -> {
            college = new College();

            session.persist( college );

            for(int k =0; k< NUMBER_OF_ENTRIES; k++){
                String name = "student"+k;
                Student student = new Student();
                student.setName(name);
                student.setCollege(college);
                college.getStudents().add(student);
                session.persist(student);
            }
            session.persist(college);
        } );
    }


    @Test
    @TestForIssue( jiraKey = "HHH-13790" )
    @AllowSysOut
    public void testConnectionLeakWhenExceptionThrown() throws Exception {
        doInHibernate( this::sessionFactory, session -> {
            loadedCollege = session.load(College.class, college.getId());
        });

        final List<Callable<String>> futureTasks = new ArrayList<>(NUMBER_OF_THREADS);
        for (int i = 0; i < NUMBER_OF_THREADS; i++) {
            futureTasks.add(() -> {
                        doInHibernate(this::sessionFactory, session -> {
                            loadedCollege.getStudents().iterator().next().getName();
                        });
                        return "";
                    }
            );

        }
        List<Future<String>> futures = executorService.invokeAll(futureTasks);
        Thread.sleep(100);
        for (Future<String> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
        }
        AbstractPersistentCollection students = ( AbstractPersistentCollection )(loadedCollege.getStudents());
        Assert.assertNull(students.getSession());
    }

    @Entity( name = "Student" )
    @Table( name = "student" )
    public static class Student {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String name;

        @ManyToOne
        private College college;

        public College getCollege() {
            return college;
        }

        public void setCollege(College college) {
            this.college = college;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
    @Entity( name = "College" )
    @Table( name = "college" )
    public static class College {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @OneToMany(fetch = FetchType.LAZY, mappedBy = "college")
        private Set<Student> students = new HashSet<>();

        public long getId() {
            return id;
        }

        public Set<Student> getStudents() {
            return students;
        }

        public void setStudents(Set<Student> students) {
            this.students = students;
        }
    }


}
