/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;
import java.util.HashMap;
import java.util.Map;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * This tests issues HHH-11624. The fix is also for HHH-10747 (and HHH-11476) and is a change on the enhanced setter.
 *
 * @author Luis Barreiro
 */

@TestForIssue( jiraKey = "HHH-10747" )
@RunWith( BytecodeEnhancerRunner.class )
public class LazyLoadingByEnhancerSetterTest extends BaseCoreFunctionalTestCase {

    private Item item, mergedItem;

    @Override
    public Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{ItemField.class, ItemProperty.class};
    }

    @Override
    protected void configure(Configuration configuration) {
        configuration.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false" );
        configuration.setProperty( Environment.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
    }

    @Before
    public void prepare() {

    }

    @Test
    public void testField() {
        doInHibernate( this::sessionFactory, s -> {
            ItemField input = new ItemField();
            input.name = "F";
            input.parameters = new HashMap<>();
            input.parameters.put( "aaa", "AAA" );
            input.parameters.put( "bbb", "BBB" );
            s.persist( input );
        } );

        doInHibernate( this::sessionFactory, s -> {
            // A parameters map is created with the class and is being compared to the persistent map (by the generated code) -- it shouldn't
            item = s.find( ItemField.class, "F" );
        } );

        doInHibernate( this::sessionFactory, s -> {
            mergedItem = (Item) s.merge( item );
        } );

        Assert.assertEquals( 2, mergedItem.getParameters().size() );
    }

    @Test
    @FailureExpected( jiraKey = "HHH-10747" )
    public void testProperty() {
        doInHibernate( this::sessionFactory, s -> {
            ItemProperty input = new ItemProperty();
            input.setName( "P" );
            Map<String, String> parameters = new HashMap<>();
            parameters.put( "ccc", "CCC" );
            parameters.put( "ddd", "DDD" );
            input.setParameters( parameters );
            s.persist( input );
        } );

        doInHibernate( this::sessionFactory, s -> {
            // A parameters map is created with the class and is being compared to the persistent map (by the generated code) -- it shouldn't
            item = s.find( ItemProperty.class, "P" );
        } );

        doInHibernate( this::sessionFactory, s -> {
            mergedItem = (Item) s.merge( item );
        } );

        Assert.assertEquals( 2, mergedItem.getParameters().size() );
    }

    // --- //

    private interface Item {
        Map<String, String> getParameters();
    }

    @Entity
    @Table( name = "ITEM_F" )
    private static class ItemField implements Item {

        @Id
        @Column( nullable = false )
        private String name;

        @ElementCollection( fetch = FetchType.EAGER )
        @MapKeyColumn( name = "NAME" )
        @Lob
        @Column( name = "VALUE", length = 65535 )
        private Map<String, String> parameters = new HashMap<>();

        @Override
        public Map<String, String> getParameters() {
            return parameters;
        }
    }

    @Entity
    @Table( name = "ITEM_P" )
    private static class ItemProperty implements Item {

        private String aName;

        private Map<String, String> parameterMap = new HashMap<>();

        @Id
        @Column( nullable = false )
        public String getName() {
            return aName;
        }

        public void setName(String name) {
            this.aName = name;
        }

        @ElementCollection( fetch = FetchType.EAGER )
        @MapKeyColumn( name = "NAME" )
        @Lob
        @Column( name = "VALUE", length = 65535 )
        @Override
        public Map<String, String> getParameters() {
            return parameterMap;
        }

        public void setParameters(Map<String, String> parameters) {
            this.parameterMap = parameters;
        }
    }
}
