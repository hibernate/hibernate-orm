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
package org.hibernate.test.bytecode.enhancement.transform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Opcode;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;

import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

/**
 * @author James Gilbertson
 */
public class TransformEnhancerTest extends BaseUnitTestCase {
	private static EnhancementContext enhancementContext = new EnhancementContext() {
		@Override
		public ClassLoader getLoadingClassLoader() {
			return getClass().getClassLoader();
		}

		@Override
		public boolean isEntityClass(CtClass classDescriptor) {
			return classDescriptor.hasAnnotation(Entity.class);
		}

		@Override
		public boolean isCompositeClass(CtClass classDescriptor) {
			return classDescriptor.hasAnnotation(Embeddable.class);
		}

		@Override
		public boolean doDirtyCheckingInline(CtClass classDescriptor) {
			return true;
		}

		@Override
		public boolean hasLazyLoadableAttributes(CtClass classDescriptor) {
			return true;
		}

		@Override
		public boolean isLazyLoadable(CtField field) {
			return true;
		}

		@Override
		public boolean isMappedCollection(CtField field) {
			try {
				return (field.getAnnotation(OneToMany.class) != null ||
						field.getAnnotation(ManyToMany.class) != null ||
						field.getAnnotation(ElementCollection.class) != null);
			} catch (ClassNotFoundException e) {
				return false;
			}
		}

		@Override
		public boolean isPersistentField(CtField ctField) {
			return true;
		}

		@Override
		public CtField[] order(CtField[] persistentFields) {
			return persistentFields;
		}
	};

	private static final String STRING_DESCRIPTOR = "Ljava/lang/String;";
	private static final String ID_FIELD_READER_METHODNAME = EnhancerConstants.PERSISTENT_FIELD_READER_PREFIX + "id";

	@Test
	@TestForIssue(jiraKey = "HHH-9529")
	public void testFieldTransformation() throws Exception {
		CtClass enhancedChildCtClass = enhanceEntity(Child.class);

		ConstPool constPool = enhancedChildCtClass.getClassFile().getConstPool();
		CtMethod method = enhancedChildCtClass.getMethod("shouldNotTransformIdFieldAccess", "()" + STRING_DESCRIPTOR);
		CodeAttribute codeAttribute = method.getMethodInfo().getCodeAttribute();

		CodeIterator ci = codeAttribute.iterator();
		while (ci.hasNext()) {
			int index = ci.next();
			int instruction = ci.byteAt(index);

			if (instruction != Opcode.INVOKESPECIAL) {
				continue;
			}
			int constIndex = ci.u16bitAt(index + 1);
			// XXX: is there someway to walk the constpool and find the method index directly?
			if (!ID_FIELD_READER_METHODNAME.equals(constPool.getMethodrefName(constIndex))) {
				continue;
			}

			index = ci.next();
			instruction = ci.byteAt(index);
			constIndex = ci.u16bitAt(index + 1);
			assertTrue("getfield ChildKey.parent was incorrectly transformed", instruction == Opcode.GETFIELD);
			assertEquals("Fieldref not referencing class 'ChildKey'", ChildKey.class.getName(), constPool.getFieldrefClassName(constIndex));
			assertEquals("Fieldref not referencing field 'parent'", "parent", constPool.getFieldrefName(constIndex));
			assertEquals("Fieldref not referencing field of type 'java.lang.String'", STRING_DESCRIPTOR, constPool.getFieldrefType(constIndex));
			return;
		}

		fail("Did not find method");
	}

	private CtClass enhanceEntity(Class<?> entityClass) throws CannotCompileException, IOException {
		Enhancer enhancer = new Enhancer(enhancementContext);

		ClassPool cp = new ClassPool(false);
		CtClass originalCtClass = cp.makeClass(getClass().getClassLoader().getResourceAsStream(entityClass.getName().replace('.', '/') + ".class"));
		byte[] original = originalCtClass.toBytecode();
		byte[] enhanced = enhancer.enhance(originalCtClass.getName(), original);
		assertFalse("entity was not enhanced", Arrays.equals(original, enhanced));

		ClassLoader cl = new ClassLoader() {};
		cp = new ClassPool(false);
		cp.appendClassPath(new LoaderClassPath(cl));
		CtClass enhancedCtClass = cp.makeClass(new ByteArrayInputStream(enhanced));
		enhancedCtClass.debugWriteFile(System.getProperty("java.io.tmpdir"));

		return enhancedCtClass;
	}
}
