// $Id$
/*
* JBoss, Home of Professional Open Source
* Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.hibernate.jpamodelgen;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import javax.annotation.processing.FilerException;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;

/**
 * @author Emmanuel Bernard
 */
public class ClassWriter {

	public static void writeFile(MetaEntity entity, ProcessingEnvironment processingEnv, Context context) {
		try {
			String metaModelPackage = entity.getPackageName();

			StringBuffer body = generateBody( entity, context );

			FileObject fo = processingEnv.getFiler().createSourceFile(
					metaModelPackage + "." + entity.getSimpleName() + "_"
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
			processingEnv.getMessager().printMessage(
					Diagnostic.Kind.ERROR,
					"Problem with Processing Environment Filer: "
							+ filerEx.getMessage()
			);
		}
		catch ( IOException ioEx ) {
			processingEnv.getMessager().printMessage(
					Diagnostic.Kind.ERROR,
					"Problem opening file to write MetaModel for " + entity.getSimpleName()
							+ ioEx.getMessage()
			);
		}
	}

	/**
	 * Generate everything after import statements.
	 *
	 * @param entity The meta entity for which to write the body
	 *
	 * @return body content
	 */
	private static StringBuffer generateBody(MetaEntity entity, Context context) {

		StringWriter sw = new StringWriter();
		PrintWriter pw = null;
		try {
			pw = new PrintWriter( sw );
			// we cannot use add @Generated into the metamodel class since this would not work in a JDK 5 environment
			//pw.println( "@" + entity.importType( Generated.class.getName() ) + "(\"JPA MetaModel for " + entity.getQualifiedName() + "\")" );
			pw.println( "@" + entity.importType( "javax.persistence.metamodel.StaticMetamodel" ) + "(" + entity.getSimpleName() + ".class)" );
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
		pw.print( "public abstract class " + entity.getSimpleName() + "_" );

		final TypeMirror superClass = entity.getTypeElement().getSuperclass();
		//superclass of Object is of NoType which returns some other kind
		String superclassDeclaration = "";
		if ( superClass.getKind() == TypeKind.DECLARED ) {
			//F..king Ch...t Have those people used their horrible APIs even once?
			final Element superClassElement = ( ( DeclaredType ) superClass ).asElement();
			String superClassName = ( ( TypeElement ) superClassElement ).getQualifiedName().toString();
			if ( context.getMetaEntitiesToProcess().containsKey( superClassName )
					|| context.getMetaSuperclassAndEmbeddableToProcess().containsKey( superClassName ) ) {
				pw.print( " extends " + superClassName + "_" );
			}
		}
		pw.println( " {" );
	}
}
