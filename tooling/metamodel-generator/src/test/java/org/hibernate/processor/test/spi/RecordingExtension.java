package org.hibernate.processor.test.spi;

import jakarta.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.processor.internal.DefaultHibernateProcessorExtension;
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
 * {@link ExtensionMarker}. Otherwise it delegates to the {@link DefaultHibernateProcessorExtension}
 * the processor uses when nothing is registered, so that it does not influence the other tests.
 */
public class RecordingExtension implements HibernateProcessorExtension {

	static final String QUALIFIER = SpiQualifier.class.getName();

	private final HibernateProcessorExtension fallback = new DefaultHibernateProcessorExtension();

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
		fallback.init( processingEnvironment );
		record( "init" );
	}

	@Override
	public @Nullable String sessionQualifier(@Nullable String dataStore, AnnotationMetaEntityContext context) {
		record( "sessionQualifier:" + context.getConstructorName() + ":dataStore=" + dataStore );
		if ( isArmed() ) {
			// the context is used to choose the qualifier, here by repository
			return '@' + context.metamodel().importType( QUALIFIER ) + "(\"" + context.getConstructorName() + "\")";
		}
		return fallback.sessionQualifier( dataStore, context );
	}

	@Override
	public boolean usesConstructorInjection(AnnotationMetaEntityContext context) {
		record( "usesConstructorInjection:" + context.getConstructorName() );
		return fallback.usesConstructorInjection( context );
	}

	@Override
	public boolean isExtensionEntity(TypeElement type) {
		record( "isExtensionEntity:" + type.getSimpleName() );
		return isMarked( type ) ? type.getKind() == ElementKind.CLASS : fallback.isExtensionEntity( type );
	}

	@Override
	public boolean isExtensionRepository(TypeElement type) {
		record( "isExtensionRepository:" + type.getSimpleName() );
		return isMarked( type ) ? type.getKind() == ElementKind.INTERFACE : fallback.isExtensionRepository( type );
	}

	@Override
	public void addRepositoryMembers(TypeElement element, AnnotationMetaEntityContext context) {
		record( "addRepositoryMembers:" + element.getSimpleName() );
		if ( !isMarked( element ) ) {
			fallback.addRepositoryMembers( element, context );
			return;
		}
		// exercise the context
		record( "context.hasMember(before)=" + context.hasMember( "spiMember" ) );
		context.addMember( "spiMember", new SpiMember( context.metamodel() ) );
		record( "context.hasMember(after)=" + context.hasMember( "spiMember" ) );
		final var primary = context.primaryEntity();
		record( "context.primaryEntity=" + ( primary == null ? null : primary.getSimpleName() ) );
		record( "context.addInjectAnnotation=" + context.addInjectAnnotation() );
		record( "context.addNonnullAnnotation=" + context.addNonnullAnnotation() );
		record( "context.getAllMembers.size>0=" + !context.getAllMembers( element ).isEmpty() );
	}

	@Override
	public @Nullable SessionSetup setupRepositorySession(
			TypeElement element,
			@Nullable ExecutableElement getter,
			AnnotationMetaEntityContext context) {
		record( "setupRepositorySession:" + element.getSimpleName() + ":getter=" + getter );
		if ( isMarked( element ) && element.getKind() == ElementKind.INTERFACE ) {
			if ( element.getSimpleName().toString().startsWith( "SpiGetter" ) ) {
				// the session is not injected, but obtained with an expression
				context.setSessionGetter( SpiSessions.class.getName() + ".session()" );
			}
			else {
				context.addRepositoryConstructor( "getSpiSession", "org.hibernate.Session" );
			}
			return new SessionSetup( "org.hibernate.Session", true );
		}
		return fallback.setupRepositorySession( element, getter, context );
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
