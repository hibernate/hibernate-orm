/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.basic;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.hibernate.annotations.Nationalized;
import org.hibernate.dialect.MySQL5Dialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.SkipForDialect;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@SkipForDialect(
        value = {
                PostgreSQL81Dialect.class,
                MySQL5Dialect.class
        },
        comment = "@see https://hibernate.atlassian.net/browse/HHH-10693 and https://hibernate.atlassian.net/browse/HHH-10695"
)
public class NClobStringTest extends BaseEntityManagerFunctionalTestCase {

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] {
            Product.class
        };
    }

    @Test
    public void test() {
        Integer productId = doInJPA( this::entityManagerFactory, entityManager -> {
            final Product product = new Product( );
            product.setId( 1 );
            product.setName( "Mobile phone" );
            product.setWarranty( "My product warranty" );

            entityManager.persist( product );
            return product.getId();
        } );
        doInJPA( this::entityManagerFactory, entityManager -> {
            Product product = entityManager.find( Product.class, productId );

            assertEquals( "My product warranty", product.getWarranty() );
        } );
    }

    //tag::basic-nclob-string-example[]
    @Entity(name = "Product")
    public static class Product {

        @Id
        private Integer id;

        private String name;

        @Lob
        @Nationalized
        private String warranty;

        //Getters and setters are omitted for brevity

    //end::basic-nclob-string-example[]
        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getWarranty() {
            return warranty;
        }

        public void setWarranty(String warranty) {
            this.warranty = warranty;
        }

        //tag::basic-nclob-string-example[]
    }
    //end::basic-nclob-string-example[]
}
