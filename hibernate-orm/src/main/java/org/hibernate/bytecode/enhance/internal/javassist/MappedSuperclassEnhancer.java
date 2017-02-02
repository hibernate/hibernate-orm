/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.javassist;

import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;

import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.hibernate.engine.spi.ManagedMappedSuperclass;

/**
 * enhancer for mapped superclass
 *
 * @author <a href="mailto:lbarreiro@redhat.com">Luis Barreiro</a>
 */
public class MappedSuperclassEnhancer extends PersistentAttributesEnhancer {

	public MappedSuperclassEnhancer(JavassistEnhancementContext context) {
		super( context );
	}

	public void enhance(CtClass managedCtClass) {
		// Add the Managed interface
		managedCtClass.addInterface( loadCtClassFromClass( ManagedMappedSuperclass.class ) );

		super.enhance( managedCtClass );
	}

	// Generate 'template' methods for each attribute. This will be overriden by the actual entities

	@Override
	protected CtMethod generateFieldReader(
			CtClass managedCtClass,
			CtField persistentField,
			AttributeTypeDescriptor typeDescriptor) {

		String fieldName = persistentField.getName();
		String readerName = EnhancerConstants.PERSISTENT_FIELD_READER_PREFIX + fieldName;

		return MethodWriter.addGetter( managedCtClass, fieldName, readerName );
	}

	@Override
	protected CtMethod generateFieldWriter(
			CtClass managedCtClass,
			CtField persistentField,
			AttributeTypeDescriptor typeDescriptor) {

		String fieldName = persistentField.getName();
		String writerName = EnhancerConstants.PERSISTENT_FIELD_WRITER_PREFIX + fieldName;

		return MethodWriter.addSetter( managedCtClass, fieldName, writerName );
	}

}
