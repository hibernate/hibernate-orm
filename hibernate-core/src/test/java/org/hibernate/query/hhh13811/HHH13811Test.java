/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.hhh13811;

import org.hibernate.QueryException;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.testing.BeforeClassOnce;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.type.Type;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Tuple;
import java.util.List;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@TestForIssue(jiraKey = "HHH-13811")
public class HHH13811Test extends BaseCoreFunctionalTestCase {

    @Test
    @RequiresDialect(H2Dialect.class)
    public void testDereferenceSuperClassAttributeInWithClause() {
        doInJPA(this::sessionFactory, em -> {
            em.createQuery("SELECT a.id FROM A a WHERE true = EXIST((SELECT 1 FROM A sub))", Tuple.class).getResultList();
        });
    }

    @BeforeClassOnce
    @SuppressWarnings( {"UnusedDeclaration"})
    protected void buildSessionFactory() {
        buildSessionFactory( cfg -> {
            cfg.addSqlFunction("exist", new SQLFunction() {
                @Override
                public boolean hasArguments() {
                    return true;
                }

                @Override
                public boolean hasParenthesesIfNoArguments() {
                    return true;
                }

                @Override
                public Type getReturnType(Type firstArgumentType, Mapping mapping) throws QueryException {
                    return ((SessionFactoryImplementor) mapping).getTypeHelper().basic(Boolean.class);
                }

                @Override
                public String render(Type firstArgumentType, List arguments, SessionFactoryImplementor factory) throws QueryException {
                    return "case when exists" + arguments.get(0).toString() + " then true else false end";
                }
            });
        } );
    }

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] { A.class };
    }

    @Entity(name = "A")
    public static class A {

        @Id
        @Column
        Long id;

    }

}
