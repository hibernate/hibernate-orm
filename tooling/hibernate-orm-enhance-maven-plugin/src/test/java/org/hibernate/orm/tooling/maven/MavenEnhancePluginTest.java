/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.tooling.maven;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.ReflectionUtils;
import org.hibernate.engine.spi.Managed;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.plexus.build.incremental.DefaultBuildContext;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

/**
 * Test case for Maven Enhance Plugin
 *
 * @author Luis Barreiro
 */
public class MavenEnhancePluginTest {

    @Test
    public void testEnhancePlugin() throws Exception {
        File baseDir = new File("target/classes/test");
        URL[] baseURLs = { baseDir.toURI().toURL() };

        MavenEnhancePlugin plugin = new MavenEnhancePlugin();

        Map<String, Object> pluginContext = new HashMap<>();
        pluginContext.put( "project", new MavenProject() );

        setVariableValueToObject( plugin, "pluginContext", pluginContext );
        setVariableValueToObject( plugin, "buildContext", new DefaultBuildContext() );

        setVariableValueToObject( plugin, "base", baseDir.getAbsolutePath() );
        setVariableValueToObject( plugin, "dir", baseDir.getAbsolutePath() );

        setVariableValueToObject( plugin, "failOnError", true );
        setVariableValueToObject( plugin, "enableLazyInitialization", true );
        setVariableValueToObject( plugin, "enableDirtyTracking", true );
        setVariableValueToObject( plugin, "enableAssociationManagement", true );
        setVariableValueToObject( plugin, "enableExtendedEnhancement", false );

        plugin.execute();

        try ( URLClassLoader classLoader = new URLClassLoader( baseURLs , getClass().getClassLoader() ) ) {

            Assert.assertTrue( declaresManaged( classLoader.loadClass( ParentEntity.class.getName() ) ) );
            Assert.assertTrue( declaresManaged( classLoader.loadClass( ChildEntity.class.getName() ) ) );
            Assert.assertTrue( declaresManaged( classLoader.loadClass( TestEntity.class.getName() ) ) );

        }

    }

    private void setVariableValueToObject( Object object, String variable, Object value ) throws IllegalAccessException {
        Field field = ReflectionUtils.getFieldByNameIncludingSuperclasses( variable, object.getClass() );
        field.setAccessible( true );
        field.set( object, value );
    }

    private boolean declaresManaged(Class<?> clazz) {
        for ( Class<?> interfaceClazz : clazz.getInterfaces() ) {
            if ( Managed.class.isAssignableFrom( interfaceClazz ) ) {
                return true;
            }
        }
        return false;
    }

    // --- //

    @MappedSuperclass
    public static class ParentEntity {

        String parentValue;

    }

    @MappedSuperclass
    public static class ChildEntity extends ParentEntity {

        String childValue;

    }

    @Entity
    public static class TestEntity extends ChildEntity {

        @Id
        long id;

        String testValue;

    }

}
