/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.javassist;

import java.io.FileNotFoundException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import javassist.CtClass;

import org.hibernate.bytecode.enhance.internal.javassist.EnhancerImpl;
import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class EnhancerFileNotFoundTest {

    @Test
    @TestForIssue( jiraKey = "HHH-11307" )
    public void test() throws Exception {
        Enhancer enhancer = new Enhancer( new DefaultEnhancementContext() );
        try {
            String resourceName = Hidden.class.getName().replace( '.', '/' ) + ".class";
            URL url = getClass().getClassLoader().getResource( resourceName );
            if ( url != null ) {
                Files.delete( Paths.get( url.toURI() ) );
                enhancer.loadCtClassFromClass( Hidden.class );
            }
            fail( "Should throw FileNotFoundException!" );
        } catch ( Exception expected ) {
            assertSame( FileNotFoundException.class, expected.getCause().getClass() );
        }
    }

    // --- //

    private static class Enhancer extends EnhancerImpl {

        public Enhancer(EnhancementContext enhancementContext) {
            super( enhancementContext );
        }

        @Override
        // change visibility protected -> public
        public CtClass loadCtClassFromClass(Class<?> aClass) {
            return super.loadCtClassFromClass( aClass );
        }
    }

    // --- //

    private static class Hidden {
    }
}
