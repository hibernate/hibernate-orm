package org.hibernate.metamodel.source.annotations.xml.mocker;

import java.util.HashMap;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import org.hibernate.metamodel.source.annotation.xml.XMLPersistenceUnitDefaults;
import org.hibernate.metamodel.source.annotations.xml.PseudoJpaDotNames;

/**
 * @author Strong Liu
 */
class PersistenceMetadataMocker extends AbstractMocker {
	private XMLPersistenceUnitDefaults persistenceUnitDefaults;
	private GlobalAnnotations globalAnnotations = new GlobalAnnotations();
	private static Map<DotName, DotName> nameDotNameMap = new HashMap<DotName, DotName>();

	static {
		nameDotNameMap.put( ACCESS, PseudoJpaDotNames.DEFAULT_ACCESS );
		nameDotNameMap.put( ENTITY_LISTENERS, PseudoJpaDotNames.DEFAULT_ENTITY_LISTENERS );
		nameDotNameMap.put( POST_LOAD, PseudoJpaDotNames.DEFAULT_POST_LOAD );
		nameDotNameMap.put( POST_REMOVE, PseudoJpaDotNames.DEFAULT_POST_REMOVE );
		nameDotNameMap.put( POST_UPDATE, PseudoJpaDotNames.DEFAULT_POST_UPDATE );
		nameDotNameMap.put( POST_PERSIST, PseudoJpaDotNames.DEFAULT_POST_PERSIST );
		nameDotNameMap.put( PRE_REMOVE, PseudoJpaDotNames.DEFAULT_PRE_REMOVE );
		nameDotNameMap.put( PRE_UPDATE, PseudoJpaDotNames.DEFAULT_PRE_UPDATE );
		nameDotNameMap.put( PRE_PERSIST, PseudoJpaDotNames.DEFAULT_PRE_PERSIST );
		nameDotNameMap.put( PseudoJpaDotNames.DEFAULT_DELIMITED_IDENTIFIERS, PseudoJpaDotNames.DEFAULT_DELIMITED_IDENTIFIERS );
	}

	PersistenceMetadataMocker(IndexBuilder indexBuilder, XMLPersistenceUnitDefaults persistenceUnitDefaults) {
		super( indexBuilder );
		this.persistenceUnitDefaults = persistenceUnitDefaults;


	}

	@Override
	protected AnnotationInstance push(AnnotationInstance annotationInstance) {
		if ( annotationInstance != null ) {
			return globalAnnotations.push( annotationInstance.name(), annotationInstance );
		}
		return null;
	}

	final void process() {
		parserAccessType( persistenceUnitDefaults.getAccess(), null );
		if ( persistenceUnitDefaults.getDelimitedIdentifiers() != null ) {
			create( PseudoJpaDotNames.DEFAULT_DELIMITED_IDENTIFIERS, null );
		}
		if ( persistenceUnitDefaults.getEntityListeners() != null ) {
			new DefaultListenerMocker( indexBuilder, null ).parser( persistenceUnitDefaults.getEntityListeners() );
		}
		indexBuilder.finishGlobalConfigurationMocking( globalAnnotations );
	}

	@Override
	protected AnnotationInstance create(DotName name, AnnotationTarget target, AnnotationValue[] annotationValues) {
		DotName defaultName = nameDotNameMap.get( name );
		if ( defaultName == null ) {
			return null;
		}
		return super.create( defaultName, target, annotationValues );

	}

	class DefaultListenerMocker extends ListenerMocker {
		DefaultListenerMocker(IndexBuilder indexBuilder, ClassInfo classInfo) {
			super( indexBuilder, classInfo );
		}

		@Override
		protected AnnotationInstance push(AnnotationInstance annotationInstance) {
			return PersistenceMetadataMocker.this.push( annotationInstance );
		}

		@Override
		protected AnnotationInstance create(DotName name, AnnotationTarget target, AnnotationValue[] annotationValues) {
		   return PersistenceMetadataMocker.this.create( name,target,annotationValues );
		}

		@Override
		protected ListenerMocker createListenerMocker(IndexBuilder indexBuilder, ClassInfo classInfo) {
			return new DefaultListenerMocker( indexBuilder, classInfo );
		}
	}


}
