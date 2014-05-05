package org.hibernate.envers.event.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.transform.dom.DOMSource;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.envers.configuration.internal.AnnotationProxyBuilder;
import org.hibernate.envers.configuration.internal.AuditEntitiesConfiguration;
import org.hibernate.envers.configuration.internal.GlobalConfiguration;
import org.hibernate.envers.configuration.internal.RevisionInfoConfiguration;
import org.hibernate.envers.configuration.internal.RevisionInfoConfigurationResult;
import org.hibernate.envers.configuration.spi.AuditConfiguration;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.Metadata;
import org.hibernate.metamodel.source.internal.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.AdditionalJaxbRootProducer;
import org.hibernate.metamodel.spi.InFlightMetadataCollector;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.domain.Attribute;
import org.hibernate.metamodel.spi.domain.AttributeContainer;
import org.hibernate.xml.spi.BindResult;
import org.hibernate.xml.spi.Origin;
import org.hibernate.xml.spi.SourceType;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class EnversJaxbRootProducer implements AdditionalJaxbRootProducer {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			EnversJaxbRootProducer.class.getName()
	);

	@Override
	public List<BindResult> produceRoots(
			final InFlightMetadataCollector metadataCollector,
			final AdditionalJaxbRootProducerContext additionalJaxbRootProducerContext) {

		final AuditConfigurationContextImpl context = new AuditConfigurationContextImpl(
				metadataCollector,
				additionalJaxbRootProducerContext
		);

		final AuditConfiguration configuration = AuditConfiguration.register( context, metadataCollector);
		return Collections.unmodifiableList( context.getBindResults() );
	}


	private class AuditConfigurationContextImpl implements AuditConfiguration.AuditConfigurationContext {

		private final InFlightMetadataCollector metadataCollector;
		private final AdditionalJaxbRootProducerContext additionalJaxbRootProducerContext;
		private final AnnotationProxyBuilder annotationProxyBuilder = new AnnotationProxyBuilder();
		private final List<BindResult> bindResults = new ArrayList<BindResult>();
		private final Origin origin = new Origin( SourceType.DOM, Origin.UNKNOWN_FILE_PATH );
		private final GlobalConfiguration globalCfg;
		private final AuditEntitiesConfiguration auditEntCfg;
		private final RevisionInfoConfigurationResult revInfoCfgResult;

		AuditConfigurationContextImpl(
				final InFlightMetadataCollector metadataCollector,
				final AdditionalJaxbRootProducerContext additionalJaxbRootProducerContext) {
			this.metadataCollector = metadataCollector;
			this.additionalJaxbRootProducerContext = additionalJaxbRootProducerContext;
			this.globalCfg = new GlobalConfiguration( additionalJaxbRootProducerContext.getServiceRegistry() );
			final RevisionInfoConfiguration revInfoCfg = new RevisionInfoConfiguration( globalCfg );
			this.revInfoCfgResult = revInfoCfg.configure( metadataCollector, additionalJaxbRootProducerContext );
			this.auditEntCfg = new AuditEntitiesConfiguration( additionalJaxbRootProducerContext.getServiceRegistry(), revInfoCfgResult.getRevisionInfoEntityName() );
		}

		@Override
		public Metadata getMetadata() {
			return metadataCollector;
		}

		@Override
		public EntityBinding getEntityBinding(String entityName) {
			return metadataCollector.getEntityBinding( entityName );
		}

		@Override
		public EntityBinding getEntityBinding(final ClassInfo clazz) {
			// TODO: Is there a better way?
//		final AnnotationInstance jpaEntityAnnotation = JandexHelper.getSingleAnnotation( clazz, JPADotNames.ENTITY );
//		String entityName = JandexHelper.getValue( jpaEntityAnnotation, "name", String.class );
//		if ( entityName == null ) {
//			entityName = clazz.name().toString();
//		}
			return getEntityBinding( clazz.name().toString() );
		}

		@Override
		public IndexView getJandexIndex() {
			return additionalJaxbRootProducerContext.getJandexIndex();
		}

		@Override
		public ClassInfo getClassInfo(AttributeContainer attributeContainer) {
			return getClassInfo(
					attributeContainer.getDescriptor().getName().toString()
			);
		}

		@Override
		public ClassInfo getClassInfo(String className) {
			return getClassInfo( DotName.createSimple( className ) );
		}

		@Override
		public ClassInfo getClassInfo(DotName classDotName) {
			return getJandexIndex().getClassByName( classDotName );
		}

		@Override
		public ClassLoaderService getClassLoaderService() {
			return additionalJaxbRootProducerContext.getServiceRegistry().getService( ClassLoaderService.class );
		}

		@Override
		public <T> T getAnnotationProxy(AnnotationInstance annotationInstance, Class<T> annotationClass) {
			return annotationProxyBuilder.getAnnotationProxy(
					annotationInstance, annotationClass, getClassLoaderService()
			);
		}

		@Override
		public Map<DotName, List<AnnotationInstance>> locateAttributeAnnotations(final Attribute attribute) {
			final ClassInfo classInfo = getClassInfo( attribute.getAttributeContainer() );
			return JandexHelper.getMemberAnnotations(
					classInfo,
					attribute.getName(),
					additionalJaxbRootProducerContext.getServiceRegistry()
			);
		}

		@Override
		public GlobalConfiguration getGlobalConfiguration() {
			return globalCfg;
		}

		@Override
		public AuditEntitiesConfiguration getAuditEntitiesConfiguration() {
			return auditEntCfg;
		}

		@Override
		public RevisionInfoConfigurationResult getRevisionInfoConfigurationResult() {
			return revInfoCfgResult;
		}

		@Override
		public void addDocument(org.w3c.dom.Document document) {
			// TODO
		}

		private List<BindResult> getBindResults() {
			return bindResults;
		}
	}
}
