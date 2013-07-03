/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc..
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
package org.hibernate.metamodel.internal.source.annotations.xml.mocker;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import org.hibernate.AssertionFailure;
import org.hibernate.jaxb.spi.orm.JaxbAccessType;
import org.hibernate.jaxb.spi.orm.JaxbAttributes;
import org.hibernate.jaxb.spi.orm.JaxbEntityListeners;
import org.hibernate.jaxb.spi.orm.JaxbIdClass;
import org.hibernate.jaxb.spi.orm.JaxbPostLoad;
import org.hibernate.jaxb.spi.orm.JaxbPostPersist;
import org.hibernate.jaxb.spi.orm.JaxbPostRemove;
import org.hibernate.jaxb.spi.orm.JaxbPostUpdate;
import org.hibernate.jaxb.spi.orm.JaxbPrePersist;
import org.hibernate.jaxb.spi.orm.JaxbPreRemove;
import org.hibernate.jaxb.spi.orm.JaxbPreUpdate;

/**
 * @author Strong Liu
 */
abstract class AbstractEntityObjectMocker extends AnnotationMocker {
	private ListenerMocker listenerparse;
	protected AbstractAttributesBuilder attributesBuilder;
	protected ClassInfo classInfo;

	AbstractEntityObjectMocker(IndexBuilder indexBuilder, EntityMappingsMocker.Default defaults) {
		super( indexBuilder, defaults );
	}

	private boolean isPreProcessCalled = false;

	/**
	 * Pre-process Entity Objects to find the default {@link javax.persistence.Access} for later attributes processing.
	 */
	final void preProcess() {
		DefaultConfigurationHelper.INSTANCE.applyDefaults( getEntityElement(), getDefaults() );
		classInfo = indexBuilder.createClassInfo( getEntityElement().getClazz() );
		DotName classDotName = classInfo.name();
		if ( getEntityElement().isMetadataComplete() != null &&  getEntityElement().isMetadataComplete() ) {
			indexBuilder.metadataComplete( classDotName );
		}
		parseAccessType( getEntityElement().getAccess(), getTarget() );
		isPreProcessCalled = true;
	}

	final void process() {
		if ( !isPreProcessCalled ) {
			throw new AssertionFailure( "preProcess should be called before process" );
		}
		if ( getEntityElement().getAccess() == null ) {
			JaxbAccessType accessType = AccessHelper.getEntityAccess( getTargetName(), indexBuilder );
			if ( accessType == null ) {
				accessType = getDefaults().getAccess();
			}
			parseAccessType( accessType, getTarget() );
		}
		processExtra();
		if ( isExcludeDefaultListeners() ) {
			create( EXCLUDE_DEFAULT_LISTENERS );
		}
		if ( isExcludeSuperclassListeners() ) {
			create( EXCLUDE_SUPERCLASS_LISTENERS );
		}
		parseIdClass( getIdClass() );

		if ( getAttributes() != null ) {
			getAttributesBuilder().parse();

		}
		if ( getEntityListeners() != null ) {
			getListenerparse().parse( getEntityListeners() );
		}
		getListenerparse().parse( getPrePersist(), PRE_PERSIST );
		getListenerparse().parse( getPreRemove(), PRE_REMOVE );
		getListenerparse().parse( getPreUpdate(), PRE_UPDATE );
		getListenerparse().parse( getPostPersist(), POST_PERSIST );
		getListenerparse().parse( getPostUpdate(), POST_UPDATE );
		getListenerparse().parse( getPostRemove(), POST_REMOVE );
		getListenerparse().parse( getPostLoad(), POST_LOAD );

		indexBuilder.finishEntityObject( getTargetName(), getDefaults() );
	}

	abstract protected EntityElement getEntityElement();
	abstract protected void processExtra();
	abstract protected boolean isExcludeDefaultListeners();

	abstract protected boolean isExcludeSuperclassListeners();

	abstract protected JaxbIdClass getIdClass();

	abstract protected JaxbEntityListeners getEntityListeners();
	abstract protected JaxbPrePersist getPrePersist();

	abstract protected JaxbPreRemove getPreRemove();

	abstract protected JaxbPreUpdate getPreUpdate();

	abstract protected JaxbPostPersist getPostPersist();

	abstract protected JaxbPostUpdate getPostUpdate();

	abstract protected JaxbPostRemove getPostRemove();

	abstract protected JaxbPostLoad getPostLoad();

	abstract protected JaxbAttributes getAttributes();

	protected ListenerMocker getListenerparse() {
		if ( listenerparse == null ) {
			listenerparse = new ListenerMocker( indexBuilder, classInfo );
		}
		return listenerparse;
	}

	protected AbstractAttributesBuilder getAttributesBuilder() {
		if ( attributesBuilder == null ) {
			attributesBuilder = new AttributesBuilder(
					indexBuilder, classInfo, getEntityElement().getAccess(), getDefaults(), getAttributes()
			);
		}
		return attributesBuilder;
	}

	protected AnnotationInstance parseIdClass(JaxbIdClass idClass) {
		if ( idClass == null ) {
			return null;
		}
		String className = MockHelper.buildSafeClassName( idClass.getClazz(), getDefaults().getPackageName() );
		return create(
				ID_CLASS, MockHelper.classValueArray(
				"value", className, indexBuilder.getServiceRegistry()
		)
		);
	}


	@Override
	protected DotName getTargetName() {
		return classInfo.name();
	}

	@Override
	protected AnnotationTarget getTarget() {
		return classInfo;
	}
}
