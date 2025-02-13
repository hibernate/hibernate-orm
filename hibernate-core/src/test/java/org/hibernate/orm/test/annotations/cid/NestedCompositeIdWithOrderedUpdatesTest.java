/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cid;


import jakarta.persistence.*;
import org.hibernate.cfg.BatchSettings;
import org.hibernate.testing.orm.junit.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Torsten Landmann
 * @author Jan Schatteman
 */
@ServiceRegistry(
        settings = {
                @Setting(name = BatchSettings.ORDER_UPDATES, value = "true")
        }
)
@DomainModel(
        annotatedClasses = {
                NestedCompositeIdWithOrderedUpdatesTest.A.class,
                NestedCompositeIdWithOrderedUpdatesTest.AId.class,
                NestedCompositeIdWithOrderedUpdatesTest.B.class,
                NestedCompositeIdWithOrderedUpdatesTest.BId.class,
                NestedCompositeIdWithOrderedUpdatesTest.C.class
        }
)
@SessionFactory
public class NestedCompositeIdWithOrderedUpdatesTest {

    @Test
    public void testUpdateOrderingWithNestedCompositeIds(SessionFactoryScope scope) {
        scope.inTransaction(
                session -> {
                    // set up entities
                    C c1 = new C();
                    c1.setCvalue("sample_c1");
                    session.persist(c1);
                    C c2 = new C();
                    c2.setCvalue("sample_c2");
                    session.persist(c2);

                    B b1 = new B();
                    b1.setId(new BId(c1, "b1_key"));
                    b1.setBvalue("sample_b1");
                    session.persist(b1);
                    B b2 = new B();
                    b2.setId(new BId(c2, "b2_key"));
                    b2.setBvalue("sample_b2");
                    session.persist(b2);

                    A a1 = new A();
                    a1.setId(new AId(b1, "a1"));
                    a1.setAvalue("sample_a1");
                    session.persist(a1);
                    A a2 = new A();
                    a2.setId(new AId(b2, "a2"));
                    a2.setAvalue("sample_a2");
                    session.persist(a2);
                }
        );

        try {
            scope.inTransaction(
                    session -> {
                        TypedQuery<A> query = session.createQuery("select a from A a", A.class);
                        List<A> aList = query.getResultList();
                        for (A curA : aList) {
                            curA.setAvalue(curA.getAvalue() + "_modified");
                            session.persist(curA);
                        }
                    }
            );
        } catch (UnsupportedOperationException uoe) {
            fail("Shouldn't throw an UnsupportedOperationException!");
        }
    }

    @Entity(name = "A")
    public static class A
    {
        @EmbeddedId
        private AId id;

        private String avalue;

        public AId getId()
        {
            return id;
        }

        public void setId(AId id)
        {
            this.id=id;
        }

        public String getAvalue()
        {
            return avalue;
        }

        public void setAvalue(String avalue)
        {
            this.avalue=avalue;
        }
    }

    @Embeddable
    public static class AId
    {
        @ManyToOne(cascade={},		// cascade nothing
                fetch=FetchType.LAZY,
                optional=false)
        private B b;

        /**
         *	"key" won't work because h2 database considers it a reserved word, and hibernate doesn't escape it.
         *	furthermore, the variable name must be after {@link #b}, alphabetically, to account for hibernate's internal sorting.
         */
        private String zkey;

        public AId()
        {
        }

        public AId(B b, String key)
        {
            this.b=b;
            this.zkey=key;
        }

        public B getB()
        {
            return b;
        }

        public void setB(B b)
        {
            this.b=b;
        }

        public String getZkey()
        {
            return zkey;
        }

        public void setZkey(String zkey)
        {
            this.zkey=zkey;
        }
    }

    @Entity(name = "B")
    public static class B
    {
        @EmbeddedId
        private BId id;

        private String bvalue;

        public BId getId()
        {
            return id;
        }

        public void setId(BId id)
        {
            this.id=id;
        }

        public String getBvalue()
        {
            return bvalue;
        }

        public void setBvalue(String bvalue)
        {
            this.bvalue=bvalue;
        }
    }

    @Embeddable
    public static class BId
    {
        @ManyToOne(cascade={},		// cascade nothing
                fetch=FetchType.LAZY,
                optional=false)
        private C c;

        /**
         *	"key" won't work because h2 database considers it a reserved word, and hibernate doesn't escape it.
         *	furthermore, the variable name must be after {@link #c}, alphabetically, to account for hibernate's internal sorting.
         */
        private String zkey;

        public BId()
        {
        }

        public BId(C c, String key)
        {
            this.c=c;
            this.zkey=key;
        }

        public C getC()
        {
            return c;
        }

        public void setC(C c)
        {
            this.c=c;
        }

        public String getZkey()
        {
            return zkey;
        }

        public void setZkey(String zkey)
        {
            this.zkey=zkey;
        }
    }

    @Entity(name = "C")
    public static class C
    {
        @Id
        private String cvalue;

        public String getCvalue()
        {
            return cvalue;
        }

        public void setCvalue(String cvalue)
        {
            this.cvalue=cvalue;
        }
    }
}
