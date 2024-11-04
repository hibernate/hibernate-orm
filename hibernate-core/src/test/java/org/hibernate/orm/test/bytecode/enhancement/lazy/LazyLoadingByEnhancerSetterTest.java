/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This tests issues HHH-11624. The fix is also for HHH-10747 (and HHH-11476) and is a change on the enhanced setter.
 *
 * @author Luis Barreiro
 */

@JiraKey( "HHH-10747" )
@DomainModel(
        annotatedClasses = {
                LazyLoadingByEnhancerSetterTest.ItemField.class, LazyLoadingByEnhancerSetterTest.ItemProperty.class
        }
)
@ServiceRegistry(
        settings = {
                @Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false" ),
                @Setting( name = AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, value = "true" ),
        }
)
@SessionFactory
@BytecodeEnhanced
public class LazyLoadingByEnhancerSetterTest {

    private Item item, mergedItem;

    @Test
    public void testField(SessionFactoryScope scope) {
        scope.inTransaction( s -> {
            ItemField input = new ItemField();
            input.name = "F";
            input.parameters = new HashMap<>();
            input.parameters.put( "aaa", "AAA" );
            input.parameters.put( "bbb", "BBB" );
            s.persist( input );
        } );

        scope.inTransaction( s -> {
            // A parameters map is created with the class and is being compared to the persistent map (by the generated code) -- it shouldn't
            item = s.find( ItemField.class, "F" );
        } );

        scope.inTransaction( s -> {
            mergedItem = (Item) s.merge( item );
        } );

        assertEquals( 2, mergedItem.getParameters().size() );
    }

    @Test
    // failure doesn't occur with HHH-16572 change @FailureExpected( jiraKey = "HHH-10747" )
    public void testProperty(SessionFactoryScope scope) {
        scope.inTransaction( s -> {
            ItemProperty input = new ItemProperty();
            input.setName( "P" );
            Map<String, String> parameters = new HashMap<>();
            parameters.put( "ccc", "CCC" );
            parameters.put( "ddd", "DDD" );
            input.setParameters( parameters );
            s.persist( input );
        } );

        scope.inTransaction( s -> {
            // A parameters map is created with the class and is being compared to the persistent map (by the generated code) -- it shouldn't
            item = s.find( ItemProperty.class, "P" );
        } );

        scope.inTransaction( s -> {
            mergedItem = (Item) s.merge( item );
        } );

        assertEquals( 2, mergedItem.getParameters().size() );
    }

    // --- //

    private interface Item {
        Map<String, String> getParameters();
    }

    @Entity
    @Table( name = "ITEM_F" )
    static class ItemField implements Item {

        @Id
        @Column( nullable = false )
        private String name;

        @ElementCollection( fetch = FetchType.EAGER )
        @MapKeyColumn( name = "NAME" )
        @Lob
        @Column( name = "PARAM_VAL", length = 65535 )
        private Map<String, String> parameters = new HashMap<>();

        @Override
        public Map<String, String> getParameters() {
            return parameters;
        }
    }

    @Entity
    @Table( name = "ITEM_P" )
    static class ItemProperty implements Item {

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
        @Column( name = "PARAM_VAL", length = 65535 )
        @Override
        public Map<String, String> getParameters() {
            return parameterMap;
        }

        public void setParameters(Map<String, String> parameters) {
            this.parameterMap = parameters;
        }
    }
}
