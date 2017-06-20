/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.metamodel;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EmbeddableMetaModelTest extends BaseEntityManagerFunctionalTestCase {
    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class[]{
            ProductEntity.class
        };
    }

    @Test
    @TestForIssue(jiraKey = "HHH-11111")
    public void testEmbeddableCanBeResolvedWhenUsedAsInterface() {
        doInJPA( this::entityManagerFactory, entityManager -> {
            assertNotNull(entityManager.getMetamodel().embeddable(LocalizedValue.class));
            assertEquals( LocalizedValue.class, ProductEntity_.description.getElementType().getJavaType() );
            assertNotNull( LocalizedValue_.value );
        } );
    }

}
