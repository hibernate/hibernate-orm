/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.basic;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.sql.NClob;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.hibernate.Session;
import org.hibernate.annotations.Nationalized;
import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.dialect.MySQL5Dialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.engine.jdbc.NClobProxy;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.SkipForDialect;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@SkipForDialect(
        value = {
                PostgreSQL81Dialect.class,
                MySQL5Dialect.class,
                AbstractHANADialect.class
        },
        comment = "@see https://hibernate.atlassian.net/browse/HHH-10693 and https://hibernate.atlassian.net/browse/HHH-10695"
)
public class NClobTest extends BaseEntityManagerFunctionalTestCase {

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] {
            Product.class
        };
    }

    @Test
    public void test() {
        Integer productId = doInJPA( this::entityManagerFactory, entityManager -> {
            Session session = entityManager.unwrap( Session.class );

            //tag::basic-nclob-persist-example[]
            String warranty = "My product warranty";

            final Product product = new Product();
            product.setId( 1 );
            product.setName( "Mobile phone" );

            product.setWarranty( NClobProxy.generateProxy( warranty ) );

            entityManager.persist( product );
            //end::basic-nclob-persist-example[]

            return product.getId();
        } );
        doInJPA( this::entityManagerFactory, entityManager -> {
            try {
                //tag::basic-nclob-find-example[]

                Product product = entityManager.find( Product.class, productId );

                try (Reader reader = product.getWarranty().getCharacterStream()) {
                    assertEquals( "My product warranty", toString( reader ) );
                }
                //end::basic-nclob-find-example[]
            }
            catch (Exception e) {
                fail( e.getMessage() );
            }
        } );
    }

    private String toString(Reader reader) throws IOException {
        BufferedReader bufferedReader = new BufferedReader( reader);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        int result = bufferedReader.read();

        while(result != -1) {
            byteArrayOutputStream.write((byte) result);
            result = bufferedReader.read();
        }

        return byteArrayOutputStream.toString();
    }


    //tag::basic-nclob-example[]
    @Entity(name = "Product")
    public static class Product {

        @Id
        private Integer id;

        private String name;

        @Lob
        @Nationalized
        // Clob also works, because NClob extends Clob.
        // The database type is still NCLOB either way and handled as such.
        private NClob warranty;

        //Getters and setters are omitted for brevity

    //end::basic-nclob-example[]
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

        public NClob getWarranty() {
            return warranty;
        }

        public void setWarranty(NClob warranty) {
            this.warranty = warranty;
        }

        //tag::basic-nclob-example[]
    }
    //end::basic-nclob-example[]
}
