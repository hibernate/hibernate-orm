/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.bytecode.enhancement;

import com.sun.tools.classfile.ConstantPoolException;
import com.sun.tools.javap.JavapTask;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;
import org.hibernate.LockMode;
import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.hibernate.engine.spi.CompositeOwner;
import org.hibernate.engine.spi.CompositeTracker;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.hibernate.engine.spi.Status;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * utility class to use in bytecode enhancement tests
 *
 * @author Steve Ebersole
 */
public class EnhancerTestUtils extends BaseUnitTestCase {

	private static EnhancementContext enhancementContext = new DefaultEnhancementContext();

    private static String workingDir = System.getProperty("java.io.tmpdir");

    /**
     * method that performs the enhancement of a class
     * also checks the signature of enhanced entities methods using 'javap' decompiler
     */
    static Class<?> enhanceAndDecompile(Class<?> classToEnhance, ClassLoader cl) throws Exception {
        CtClass entityCtClass = generateCtClassForAnEntity(classToEnhance);

        byte[] original = entityCtClass.toBytecode();
        byte[] enhanced = new Enhancer(enhancementContext).enhance(entityCtClass.getName(), original);
        assertFalse("entity was not enhanced", Arrays.equals(original, enhanced));
        System.out.printf("enhanced entity [%s]", entityCtClass.getName());

        ClassPool cp = new ClassPool(false);
        cp.appendClassPath(new LoaderClassPath(cl));
        CtClass enhancedCtClass = cp.makeClass(new ByteArrayInputStream(enhanced));

        enhancedCtClass.debugWriteFile(workingDir);
        decompileDumpedClass(classToEnhance.getName());

        Class<?> enhancedClass = enhancedCtClass.toClass(cl, EnhancerTestUtils.class.getProtectionDomain());
        assertNotNull(enhancedClass);
        return enhancedClass;
    }

    private static void decompileDumpedClass(String className) {
        try {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singletonList(new File(workingDir)));

            JavapTask javapTask = new JavapTask();
            for (JavaFileObject jfo : fileManager.getJavaFileObjects(workingDir + File.separator + getFilenameForClassName(className))) {
                try {
                    Set<String> interfaceNames = new HashSet<String>();
                    Set<String> fieldNames = new HashSet<String>();
                    Set<String> methodNames = new HashSet<String>();

                    JavapTask.ClassFileInfo info = javapTask.read(jfo);

                    System.out.println();
                    System.out.println("decompiled class= " + info.cf.getName());

                    for (int i : info.cf.interfaces) {
                        interfaceNames.add(info.cf.constant_pool.getClassInfo(i).getName());
                        System.out.println("declared iFace  = " + info.cf.constant_pool.getClassInfo(i).getName());
                    }
                    for (com.sun.tools.classfile.Field f : info.cf.fields) {
                        fieldNames.add(f.getName(info.cf.constant_pool));
                        System.out.println("declared field  = " + f.getName(info.cf.constant_pool));
                    }
                    for (com.sun.tools.classfile.Method m : info.cf.methods) {
                        methodNames.add(m.getName(info.cf.constant_pool));
                        System.out.println("declared method = " + m.getName(info.cf.constant_pool));
                    }
                    System.out.println();

                    // checks signature against known interfaces
                    if (interfaceNames.contains(PersistentAttributeInterceptor.class.getName())) {
                        assertTrue(fieldNames.contains(EnhancerConstants.INTERCEPTOR_FIELD_NAME));
                        assertTrue(methodNames.contains(EnhancerConstants.INTERCEPTOR_GETTER_NAME));
                        assertTrue(methodNames.contains(EnhancerConstants.INTERCEPTOR_SETTER_NAME));
                    }
                    if (interfaceNames.contains(ManagedEntity.class.getName())) {
                        assertTrue(methodNames.contains(EnhancerConstants.ENTITY_INSTANCE_GETTER_NAME));

                        assertTrue(fieldNames.contains(EnhancerConstants.ENTITY_ENTRY_FIELD_NAME));
                        assertTrue(methodNames.contains(EnhancerConstants.ENTITY_ENTRY_GETTER_NAME));
                        assertTrue(methodNames.contains(EnhancerConstants.ENTITY_ENTRY_SETTER_NAME));

                        assertTrue(fieldNames.contains(EnhancerConstants.PREVIOUS_FIELD_NAME));
                        assertTrue(methodNames.contains(EnhancerConstants.PREVIOUS_GETTER_NAME));
                        assertTrue(methodNames.contains(EnhancerConstants.PREVIOUS_SETTER_NAME));

                        assertTrue(fieldNames.contains(EnhancerConstants.NEXT_FIELD_NAME));
                        assertTrue(methodNames.contains(EnhancerConstants.NEXT_GETTER_NAME));
                        assertTrue(methodNames.contains(EnhancerConstants.NEXT_SETTER_NAME));
                    }
                    if (interfaceNames.contains(SelfDirtinessTracker.class.getName())) {
                        assertTrue(fieldNames.contains(EnhancerConstants.TRACKER_FIELD_NAME));
                        assertTrue(methodNames.contains(EnhancerConstants.TRACKER_GET_NAME));
                        assertTrue(methodNames.contains(EnhancerConstants.TRACKER_CLEAR_NAME));
                        assertTrue(methodNames.contains(EnhancerConstants.TRACKER_HAS_CHANGED_NAME));
                    }
                    if (interfaceNames.contains(CompositeTracker.class.getName())) {
                        assertTrue(fieldNames.contains(EnhancerConstants.TRACKER_COMPOSITE_FIELD_NAME));
                        assertTrue(methodNames.contains(EnhancerConstants.TRACKER_COMPOSITE_SET_OWNER));
                        assertTrue(methodNames.contains(EnhancerConstants.TRACKER_COMPOSITE_SET_OWNER));
                    }
                    if (interfaceNames.contains(CompositeOwner.class.getName())) {
                        assertTrue(fieldNames.contains(EnhancerConstants.TRACKER_CHANGER_NAME));
                        assertTrue(methodNames.contains(EnhancerConstants.TRACKER_CHANGER_NAME));
                    }
                } catch (ConstantPoolException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException ioe) {
            assertNull("Failed to open class file", ioe);
        } catch (RuntimeException re) {
            System.out.println("WARNING: UNABLE DECOMPILE DUE TO " + re.getMessage());
        }
    }

    private static CtClass generateCtClassForAnEntity(Class<?> entityClassToEnhance) throws Exception {
        ClassPool cp = new ClassPool(false);
        return cp.makeClass(EnhancerTestUtils.class.getClassLoader().getResourceAsStream(getFilenameForClassName(entityClassToEnhance.getName())));
    }

    private static String getFilenameForClassName(String className) {
        return className.replace('.', File.separatorChar) + JavaFileObject.Kind.CLASS.extension;
    }

    /**
     * clears the dirty set for an entity
     */
    public static void clearDirtyTracking (Object entityInstance) {
        try {
            entityInstance.getClass().getMethod(EnhancerConstants.TRACKER_CLEAR_NAME).invoke(entityInstance);
            checkDirtyTracking(entityInstance);
        } catch (InvocationTargetException e) {
            assertNull("Exception in clear dirty tracking", e);
        } catch (NoSuchMethodException e) {
            assertNull("Exception in clear dirty tracking", e);
        } catch (IllegalAccessException e) {
            assertNull("Exception in clear  dirty tracking", e);
        }
    }

    /**
     * compares the dirty fields of an entity with a set of expected values
     */
    public static void checkDirtyTracking (Object entityInstance, String ... dirtyFields) {
        try {
            assertTrue((dirtyFields.length == 0) != (Boolean) entityInstance.getClass().getMethod(EnhancerConstants.TRACKER_HAS_CHANGED_NAME).invoke(entityInstance));
            Set<?> tracked = (Set<?>) entityInstance.getClass().getMethod(EnhancerConstants.TRACKER_GET_NAME).invoke(entityInstance);
            assertEquals(dirtyFields.length, tracked.size());
            assertTrue(tracked.containsAll(Arrays.asList(dirtyFields)));
        } catch (InvocationTargetException e) {
            assertNull("Exception while checking dirty tracking", e);
        } catch (NoSuchMethodException e) {
            assertNull("Exception while checking dirty tracking", e);
        } catch (IllegalAccessException e) {
            assertNull("Exception while checking dirty tracking", e);
        }
    }

    static EntityEntry makeEntityEntry() {
        return new EntityEntry(
                Status.MANAGED,
                null,
                null,
                1,
                null,
                LockMode.NONE,
                false,
                null,
                false,
                false,
                null
        );
    }

    public static class LocalPersistentAttributeInterceptor implements PersistentAttributeInterceptor {

        @Override public boolean readBoolean(Object obj, String name, boolean oldValue) {
            System.out.println( "Reading boolean [" + name + "]" );
            return oldValue;
        }
        @Override public boolean writeBoolean(Object obj, String name, boolean oldValue, boolean newValue) {
            System.out.println( "Writing boolean [" + name + "]" );
            return newValue;
        }

        @Override public byte readByte(Object obj, String name, byte oldValue) {
            System.out.println( "Reading byte [" + name + "]" );
            return oldValue;
        }
        @Override public byte writeByte(Object obj, String name, byte oldValue, byte newValue) {
            System.out.println( "Writing byte [" + name + "]" );
            return newValue;
        }

        @Override public char readChar(Object obj, String name, char oldValue) {
            System.out.println( "Reading char [" + name + "]" );
            return oldValue;
        }
        @Override public char writeChar(Object obj, String name, char oldValue, char newValue) {
            System.out.println( "Writing char [" + name + "]" );
            return newValue;
        }

        @Override public short readShort(Object obj, String name, short oldValue) {
            System.out.println( "Reading short [" + name + "]" );
            return oldValue;
        }
        @Override public short writeShort(Object obj, String name, short oldValue, short newValue) {
            System.out.println( "Writing short [" + name + "]" );
            return newValue;
        }

        @Override public int readInt(Object obj, String name, int oldValue) {
            System.out.println( "Reading int [" + name + "]" );
            return oldValue;
        }
        @Override public int writeInt(Object obj, String name, int oldValue, int newValue) {
            System.out.println( "Writing int [" + name + "]" );
            return newValue;
        }

        @Override public float readFloat(Object obj, String name, float oldValue) {
            System.out.println( "Reading float [" + name + "]" );
            return oldValue;
        }
        @Override public float writeFloat(Object obj, String name, float oldValue, float newValue) {
            System.out.println( "Writing float [" + name + "]" );
            return newValue;
        }

        @Override public double readDouble(Object obj, String name, double oldValue) {
            System.out.println( "Reading double [" + name + "]" );
            return oldValue;
        }
        @Override public double writeDouble(Object obj, String name, double oldValue, double newValue) {
            System.out.println( "Writing double [" + name + "]" );
            return newValue;
        }

        @Override public long readLong(Object obj, String name, long oldValue) {
            System.out.println( "Reading long [" + name + "]" );
            return oldValue;
        }
        @Override public long writeLong(Object obj, String name, long oldValue, long newValue) {
            System.out.println( "Writing long [" + name + "]" );
            return newValue;
        }

        @Override public Object readObject(Object obj, String name, Object oldValue) {
            System.out.println( "Reading Object [" + name + "]" );
            return oldValue;
        }
        @Override public Object writeObject(Object obj, String name, Object oldValue, Object newValue) {
            System.out.println( "Writing Object [" + name + "]" );
            return newValue;
        }
    }

}
