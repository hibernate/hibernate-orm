/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.spi;

import jakarta.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.processor.model.MetaAttribute;
import org.hibernate.processor.model.Metamodel;
import org.hibernate.processor.spi.AnnotationMetaEntityContext;
import org.hibernate.processor.spi.HibernateProcessorExtension;
import org.hibernate.processor.spi.SessionSetup;

/**
 * A {@link HibernateProcessorExtension} registered with {@link java.util.ServiceLoader} for
 * the whole test source set, which records every call made to it by the processor.
 * <p>
 * It only does something when {@linkplain #arm() armed}, and then only for types annotated
 * {@link ExtensionMarker}, so that it does not influence the other tests.
 */
public class RecordingExtension implements HibernateProcessorExtension {

	static final String QUALIFIER = SpiQualifier.class.getName();

	private static final List<String> EVENTS = new ArrayList<>();
	private static boolean armed;

	static synchronized void arm() {
		armed = true;
		EVENTS.clear();
	}

	static synchronized void disarm() {
		armed = false;
		EVENTS.clear();
	}

	static synchronized List<String> events() {
		return List.copyOf( EVENTS );
	}

	private static synchronized void record(String event) {
		if ( armed ) {
			EVENTS.add( event );
		}
	}

	private static synchronized boolean isArmed() {
		return armed;
	}

	private static boolean isMarked(TypeElement type) {
		return isArmed()
			&& type.getAnnotationMirrors().stream()
				.anyMatch( mirror -> mirror.getAnnotationType().toString().equals( ExtensionMarker.class.getName() ) );
	}

	@Override
	public void init(ProcessingEnvironment processingEnvironment) {
		record( "init" );
	}

	@Override
	public @Nullable String qualifierAnnotation() {
		record( "qualifierAnnotation" );
		return isArmed() ? QUALIFIER : null;
	}

	@Override
	public boolean isExtensionEntity(TypeElement type) {
		record( "isExtensionEntity:" + type.getSimpleName() );
		return isMarked( type ) && type.getKind() == ElementKind.CLASS;
	}

	@Override
	public boolean isExtensionRepository(TypeElement type) {
		record( "isExtensionRepository:" + type.getSimpleName() );
		return isMarked( type ) && type.getKind() == ElementKind.INTERFACE;
	}

	@Override
	public void addRepositoryMembers(TypeElement element, AnnotationMetaEntityContext context) {
		record( "addRepositoryMembers:" + element.getSimpleName() );
		context.addMember( "spiMember", new SpiMember( context.metamodel() ) );
	}

	@Override
	public @Nullable SessionSetup setupRepositorySession(
			TypeElement element,
			@Nullable ExecutableElement getter,
			AnnotationMetaEntityContext context) {
		record( "setupRepositorySession:" + element.getSimpleName() + ":getter=" + getter );
		if ( isMarked( element ) && element.getKind() == ElementKind.INTERFACE ) {
			context.addRepositoryConstructor( "getSpiSession", "org.hibernate.Session" );
			return new SessionSetup( "org.hibernate.Session", true );
		}
		return null;
	}

	/** A static method added to the metamodel class by {@link #addRepositoryMembers}. */
	// not a record, whose hashCode and toString would recurse into the metamodel holding this member
	private static final class SpiMember implements MetaAttribute {
		private final Metamodel metamodel;

		private SpiMember(Metamodel metamodel) {
			this.metamodel = metamodel;
		}

		@Override
		public boolean hasTypedAttribute() {
			return true;
		}

		@Override
		public boolean hasStringAttribute() {
			return false;
		}

		@Override
		public String getAttributeDeclarationString() {
			return "public static String spiMember() { return \"added by the extension\"; }";
		}

		@Override
		public String getAttributeNameDeclarationString() {
			return "";
		}

		@Override
		public String getMetaType() {
			return "java.lang.String";
		}

		@Override
		public String getPropertyName() {
			return "spiMember";
		}

		@Override
		public String getTypeDeclaration() {
			return "java.lang.String";
		}

		@Override
		public Metamodel getHostingEntity() {
			return metamodel;
		}
	}
}
