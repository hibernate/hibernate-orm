package org.hibernate.metamodel.source.internal.jandex;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.metamodel.source.internal.jaxb.JaxbPersistenceUnitDefaults;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

/**
 * @author Strong Liu
 */
public class PersistenceMetadataMocker extends AbstractMocker {
	private final JaxbPersistenceUnitDefaults persistenceUnitDefaults;
	private final GlobalAnnotations globalAnnotations = new GlobalAnnotations();
	/**
	 * Map JPA Annotations name to Pseudo JPA Annotations name.
	 */
	private final static Map<DotName, DotName> nameMapper = new HashMap<DotName, DotName>();

	static {
		nameMapper.put( ACCESS, PseudoJpaDotNames.DEFAULT_ACCESS );
		nameMapper.put( ENTITY_LISTENERS, PseudoJpaDotNames.DEFAULT_ENTITY_LISTENERS );
		nameMapper.put( POST_LOAD, PseudoJpaDotNames.DEFAULT_POST_LOAD );
		nameMapper.put( POST_REMOVE, PseudoJpaDotNames.DEFAULT_POST_REMOVE );
		nameMapper.put( POST_UPDATE, PseudoJpaDotNames.DEFAULT_POST_UPDATE );
		nameMapper.put( POST_PERSIST, PseudoJpaDotNames.DEFAULT_POST_PERSIST );
		nameMapper.put( PRE_REMOVE, PseudoJpaDotNames.DEFAULT_PRE_REMOVE );
		nameMapper.put( PRE_UPDATE, PseudoJpaDotNames.DEFAULT_PRE_UPDATE );
		nameMapper.put( PRE_PERSIST, PseudoJpaDotNames.DEFAULT_PRE_PERSIST );
		nameMapper.put(
				PseudoJpaDotNames.DEFAULT_DELIMITED_IDENTIFIERS,
				PseudoJpaDotNames.DEFAULT_DELIMITED_IDENTIFIERS
		);
	}

	PersistenceMetadataMocker(IndexBuilder indexBuilder, JaxbPersistenceUnitDefaults persistenceUnitDefaults,
			Default defaults) {
		super( indexBuilder, defaults );
		this.persistenceUnitDefaults = persistenceUnitDefaults;
	}

	/**
	 * Mock global configurations defined in <persistence-unit-metadata> with pseudo JPA annotation name.
	 * NOTE: These mocked annotations do not have {@link AnnotationTarget target}.
	 */
	final void process() {
		parseAccessType( persistenceUnitDefaults.getAccess(), null );
		if ( persistenceUnitDefaults.getDelimitedIdentifiers() != null ) {
			create( PseudoJpaDotNames.DEFAULT_DELIMITED_IDENTIFIERS, null );
		}
		if ( persistenceUnitDefaults.getEntityListeners() != null ) {

			new DefaultListenerMocker( indexBuilder, null, getDefaults() ).parse( persistenceUnitDefaults.getEntityListeners() );
		}
		indexBuilder.finishGlobalConfigurationMocking( globalAnnotations );
	}

	@Override
	protected AnnotationInstance push(AnnotationInstance annotationInstance) {
		if ( annotationInstance != null ) {
			return globalAnnotations.push( annotationInstance.name(), annotationInstance );
		}
		return null;
	}

	@Override
	protected AnnotationInstance create(DotName name, AnnotationTarget target, AnnotationValue[] annotationValues) {
		DotName defaultName = nameMapper.get( name );
		if ( defaultName == null ) {
			return null;
		}
		return super.create( defaultName, target, annotationValues );

	}

	private class DefaultListenerMocker extends ListenerMocker {
		DefaultListenerMocker(IndexBuilder indexBuilder, ClassInfo classInfo, Default defaults) {
			super( indexBuilder, classInfo, defaults );
		}

		@Override
		protected AnnotationInstance push(AnnotationInstance annotationInstance) {
			return PersistenceMetadataMocker.this.push( annotationInstance );
		}

		@Override
		protected AnnotationInstance create(DotName name, AnnotationTarget target, AnnotationValue[] annotationValues) {
			return PersistenceMetadataMocker.this.create( name, target, annotationValues );
		}

		@Override
		protected ListenerMocker createListenerMocker(IndexBuilder indexBuilder, ClassInfo classInfo) {
			return new DefaultListenerMocker( indexBuilder, classInfo, getDefaults() );
		}
	}
}
