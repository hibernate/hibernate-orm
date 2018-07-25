package org.hibernate.test;

import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinColumnsOrFormulas;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

@TestForIssue(jiraKey = "HHH-12770")
public class HHH12770Test extends BaseEntityManagerFunctionalTestCase {

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{
                Stock.class,
                Code.class,
        };
    }

    @Override
    protected void addConfigOptions(Map options) {
        options.put(
                AvailableSettings.SHOW_SQL,
                Boolean.TRUE
        );
    }

    @Before
    public void setUp() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            Code code = new Code();
            code.setId((long)1);
            code.setCopeType(CodeType.TYPE_A);
            entityManager.persist(code);


            Stock stock1 = new Stock();
            stock1.setId((long) 1);
            stock1.setCode(code);
            entityManager.persist(stock1);

            Stock stock2 = new Stock();
            stock2.setId((long) 2);
            entityManager.persist(stock2);

        });
    }


    @Test
    public void testSelectAll() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Stock> stocks = entityManager.createQuery(
                    "  SELECT s FROM Stock s  ")
                    .getResultList();

            assertEquals(2, stocks.size());
        });

    }


    @Entity(name = "Stock")
    public static class Stock implements Serializable {

        @Id
        @Column(name = "ID")
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        @NotFound(action = NotFoundAction.IGNORE)
        @JoinColumnsOrFormulas({@JoinColumnOrFormula(column = @JoinColumn(name = "CODE_ID", referencedColumnName = "ID")),
                @JoinColumnOrFormula(formula = @JoinFormula(referencedColumnName = "TYPE", value = "'TYPE_A'"))})
        private Code code;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Code getCode() {
            return code;
        }

        public void setCode(Code code) {
            this.code = code;
        }
    }

    @Entity(name = "Code")
    public static class Code implements Serializable {

        @Id
        @Column(name = "ID")
        private Long id;

        @Id
        @Enumerated(EnumType.STRING)
        @Column(name = "TYPE")
        private CodeType copeType;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public CodeType getCopeType() {
            return copeType;
        }

        public void setCopeType(CodeType copeType) {
            this.copeType = copeType;
        }
    }

    public enum CodeType {
        TYPE_A, TYPE_B;
    }

}
