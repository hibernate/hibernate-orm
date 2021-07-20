package org.hibernate.test.annotations.formula;

import org.hibernate.annotations.JoinFormula;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SharedCacheMode;
import javax.persistence.Table;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class JoinFormulaWithOneToMany extends BaseUnitTestCase {

    @Test
    @TestForIssue(jiraKey = "HHH-12997")
    @FailureExpected(jiraKey = "HHH-12997")
    public void testJoinFormulaWithOneToMany() {

        PersistenceUnitInfo info = new PersistenceUnitInfo() {
            @Override
            public String getPersistenceUnitName() {
                return "test";
            }

            @Override
            public String getPersistenceProviderClassName() {
                return null;
            }

            @Override
            public PersistenceUnitTransactionType getTransactionType() {
                return null;
            }

            @Override
            public DataSource getJtaDataSource() {
                return null;
            }

            @Override
            public DataSource getNonJtaDataSource() {
                return null;
            }

            @Override
            public List<String> getMappingFileNames() {
                return null;
            }

            @Override
            public List<URL> getJarFileUrls() {
                return null;
            }

            @Override
            public URL getPersistenceUnitRootUrl() {
                return null;
            }

            @Override
            public List<String> getManagedClassNames() {
                return Arrays.asList(Parent.class.getName(), Child.class.getName());
            }

            @Override
            public boolean excludeUnlistedClasses() {
                return false;
            }

            @Override
            public SharedCacheMode getSharedCacheMode() {
                return null;
            }

            @Override
            public ValidationMode getValidationMode() {
                return null;
            }

            @Override
            public Properties getProperties() {
                return new Properties();
            }

            @Override
            public String getPersistenceXMLSchemaVersion() {
                return null;
            }

            @Override
            public ClassLoader getClassLoader() {
                return null;
            }

            @Override
            public void addTransformer(ClassTransformer transformer) {

            }

            @Override
            public ClassLoader getNewTempClassLoader() {
                return null;
            }

        };

        new EntityManagerFactoryBuilderImpl(new PersistenceUnitInfoDescriptor(info),
                                            Collections.singletonMap("hibernate.dialect", "org.hibernate.dialect.H2Dialect")).build();

    }

    @Entity @Table(name = "child") class Child {

        @Id @GeneratedValue(strategy = GenerationType.AUTO) private Long id;

        @ManyToOne() @JoinFormula(value = "(SELECT * FROM child)") private Parent parent;

    }

    @Entity @Table(name = "parent") class Parent {

        @Id @GeneratedValue(strategy = GenerationType.AUTO) private Long id;

        @OneToMany(mappedBy = "parent") private Set<Child> childs;

    }

}
