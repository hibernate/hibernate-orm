/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.tooling.gradle

import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.ManyToMany
import javax.persistence.OneToMany
import javax.persistence.Transient

import javassist.ClassPool
import javassist.CtClass
import javassist.CtField

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.TaskAction

import org.hibernate.bytecode.enhance.spi.EnhancementContext
import org.hibernate.bytecode.enhance.spi.Enhancer

/**
 * Gradle Task to apply Hibernate's bytecode Enhancer
 *
 * @author Jeremy Whiting
 */
public class EnhancerTask extends DefaultTask implements EnhancementContext {

    private ClassLoader overridden

    public EnhancerTask() {
        super()
        setDescription( 'Enhances Entity classes for efficient association referencing.' )
    }

    @TaskAction
    def enhance() {
        logger.info( 'enhance task started' )
        ext.pool = new ClassPool( false )
        ext.enhancer = new Enhancer( this )
        FileTree tree = project.fileTree( dir: project.sourceSets.main.output.classesDir )
        tree.include '**/*.class'
        tree.each( { File file ->
            final byte[] enhancedBytecode;
            InputStream is = null;
            CtClass clas = null;
            try {
                is = new FileInputStream( file.toString() )
                clas = ext.pool.makeClass( is )
                // Enhancer already does this check to see if it should enhance, why are we doing it again here?
                if ( !clas.hasAnnotation( Entity.class ) ) {
                    logger.debug( "Class $file not an annotated Entity class. skipping..." )
                }
                else {
                    enhancedBytecode = ext.enhancer.enhance( clas.getName(), clas.toBytecode() );
                }
            }
            catch (Exception e) {
                logger.error( "Unable to enhance class [${file.toString()}]", e )
                return
            }
            finally {
                try {
                    if ( null != is ) {
                        is.close()
                    };
                }
                finally {}
            }
            if ( null != enhancedBytecode ) {
                if ( file.delete() ) {
                    if ( !file.createNewFile() ) {
                        logger.error( "Unable to recreate class file [" + clas.getName() + "]" )
                    }
                }
                else {
                    logger.error( "Unable to delete class file [" + clas.getName() + "]" )
                }
                FileOutputStream outputStream = new FileOutputStream( file, false )
                try {
                    outputStream.write( enhancedBytecode )
                    outputStream.flush()
                }
                finally {
                    try {
                        if ( outputStream != null ) {
                            outputStream.close()
                        }
                        clas.detach()//release memory
                    }
                    catch (IOException ignore) {
                    }
                }
            }
        } )
        logger.info( 'enhance task finished' )
    }

    public ClassLoader getLoadingClassLoader() {
        if ( null == this.overridden ) {
            return getClass().getClassLoader();
        }
        else {
            return this.overridden;
        }
    }

    public void setClassLoader(ClassLoader loader) {
        this.overridden = loader;
    }

    public boolean isEntityClass(CtClass classDescriptor) {
        return true;
    }

    public boolean hasLazyLoadableAttributes(CtClass classDescriptor) {
        return true;
    }

    public boolean isLazyLoadable(CtField field) {
        return true;
    }

    public boolean isCompositeClass(CtClass classDescriptor) {
        return false;
    }

    public boolean doBiDirectionalAssociationManagement(CtField field) {
        return false;
    }

    public boolean doDirtyCheckingInline(CtClass classDescriptor) {
        return true;
    }

    public CtField[] order(CtField[] fields) {
        // TODO: load ordering from configuration.
        return fields;
    }

    public boolean isMappedCollection(CtField field) {
        try {
            return (field.getAnnotation(OneToMany.class) != null ||
                    field.getAnnotation(ManyToMany.class) != null ||
                    field.getAnnotation(ElementCollection.class) != null);
        }
        catch (ClassNotFoundException e) {
            return false;
        }
    }

    public boolean isPersistentField(CtField ctField) {
        return !ctField.hasAnnotation( Transient.class );
    }
} 
