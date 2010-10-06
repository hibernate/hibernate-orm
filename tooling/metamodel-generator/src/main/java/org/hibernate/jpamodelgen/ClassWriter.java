/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// $Id$

package org.hibernate.jpamodelgen;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import javax.annotation.Generated;
import javax.annotation.processing.FilerException;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import javax.tools.Diagnostic;
import javax.tools.FileObject;

import org.hibernate.jpamodelgen.model.MetaAttribute;
import org.hibernate.jpamodelgen.model.MetaEntity;

/**
 * Helper class to write the actual meta model class using the  {@link javax.annotation.processing.Filer} API.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public final class ClassWriter {
	private static final String META_MODEL_CLASS_NAME_SUFFIX = "_";

	private ClassWriter() {
	}

	public static void writeFile(MetaEntity entity, Context context) {
		try {
			String metaModelPackage = entity.getPackageName();
			StringBuffer body = generateBody( entity, context );

			FileObject fo = context.getProcessingEnvironment().getFiler().createSourceFile(
					getFullyQualifiedClassName( entity, metaModelPackage )
			);
			OutputStream os = fo.openOutputStream();
			PrintWriter pw = new PrintWriter( os );

			pw.println( "package " + metaModelPackage + ";" );
			pw.println();
			pw.println( entity.generateImports() );
			pw.println( body );

			pw.flush();
			pw.close();
		}
		catch ( FilerException filerEx ) {
			context.logMessage(
					Diagnostic.Kind.ERROR, "Problem with Filer: " + filerEx.getMessage()
			);
		}
		catch ( IOException ioEx ) {
			context.logMessage(
					Diagnostic.Kind.ERROR,
					"Problem opening file to write MetaModel for " + entity.getSimpleName() + ioEx.getMessage()
			);
		}
	}

	/**
	 * Generate everything after import statements.
	 *
	 * @param entity The meta entity for which to write the body
	 * @param context The processing context
	 *
	 * @return body content
	 */
	private static StringBuffer generateBody(MetaEntity entity, Context context) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = null;
		try {
			pw = new PrintWriter( sw );
			if ( context.isAddGeneratedAnnotation() ) {
				pw.println( writeGeneratedAnnotation( entity ) );
			}
			pw.println( writeStaticMetaModelAnnotation( entity ) );
			printClassDeclaration( entity, pw, context );
			pw.println();
			List<MetaAttribute> members = entity.getMembers();
			for ( MetaAttribute metaMember : members ) {
				pw.println( "	" + metaMember.getDeclarationString() );
			}
			pw.println();
			pw.println( "}" );
			return sw.getBuffer();
		}
		finally {
			if ( pw != null ) {
				pw.close();
			}
		}
	}

	private static void printClassDeclaration(MetaEntity entity, PrintWriter pw, Context context) {
		pw.print( "public abstract class " + entity.getSimpleName() + META_MODEL_CLASS_NAME_SUFFIX );

		final TypeMirror superClass = entity.getTypeElement().getSuperclass();
		//superclass of Object is of NoType which returns some other kind
		if ( superClass.getKind() == TypeKind.DECLARED ) {
			//F..king Ch...t Have those people used their horrible APIs even once?
			final Element superClassElement = ( (DeclaredType) superClass ).asElement();
			String superClassName = ( (TypeElement) superClassElement ).getQualifiedName().toString();
			if ( extendsSuperMetaModel( superClassElement, entity.isMetaComplete(), context ) ) {
				pw.print( " extends " + superClassName + META_MODEL_CLASS_NAME_SUFFIX );
			}
		}
		pw.println( " {" );
	}

	/**
	 * Checks whether this metamodel class needs to extend another metamodel class.
	 * This methods checks whether the processor has generated a metamodel class for the super class, but it also
	 * allows for the possibility that the metamodel class was generated in a previous compilation (eg it could be
	 * part of a separate jar. See also METAGEN-35).
	 *
	 * @param superClassElement the super class element
	 * @param entityMetaComplete flag indicating if the entity for which the metamodel should be generarted is metamodel
	 * complete. If so we cannot use reflection to decide whether we have to add the extend clause
	 * @param context the execution context
	 *
	 * @return {@code true} in case there is super class meta model to extend from {@code false} otherwise.
	 */
	private static boolean extendsSuperMetaModel(Element superClassElement, boolean entityMetaComplete, Context context) {
		// if we processed the superclass in the same run we definitely need to extend
		String superClassName = ( (TypeElement) superClassElement ).getQualifiedName().toString();
		if ( context.containsMetaEntity( superClassName )
				|| context.containsMetaEmbeddable( superClassName ) ) {
			return true;
		}

		// to allow for the case that the metamodel class for the super entity is for example contained in another
		// jar file we use reflection. However, we need to consider the fact that there is xml configuration
		// and annotations should be ignored
		if ( !entityMetaComplete && ( superClassElement.getAnnotation( Entity.class ) != null
				|| superClassElement.getAnnotation( MappedSuperclass.class ) != null ) ) {
			return true;
		}

		return false;
	}

	private static String getFullyQualifiedClassName(MetaEntity entity, String metaModelPackage) {
		return metaModelPackage + "." + entity.getSimpleName() + META_MODEL_CLASS_NAME_SUFFIX;
	}

	private static String writeGeneratedAnnotation(MetaEntity entity) {
		return "@" + entity.importType( Generated.class.getName() ) + "(\"JPA MetaModel for " + entity.getQualifiedName() + "\")";
	}

	private static String writeStaticMetaModelAnnotation(MetaEntity entity) {
		return "@" + entity.importType( "javax.persistence.metamodel.StaticMetamodel" ) + "(" + entity.getSimpleName() + ".class)";
	}
}
