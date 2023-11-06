/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.hibernate.boot.jaxb.mapping.spi.JaxbAttributesContainer;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAttributesContainerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbBasicImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbElementCollectionImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddedIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddedImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbLifecycleCallback;
import org.hibernate.boot.jaxb.mapping.spi.JaxbLifecycleCallbackContainer;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManagedType;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManyToManyImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManyToOneImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbOneToManyImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbOneToOneImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistentAttribute;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPostLoadImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPostPersistImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPostRemoveImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPostUpdateImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPrePersistImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPreRemoveImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPreUpdateImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTenantIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTransientImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbVersionImpl;


/**
 * Reproduces what we used to do with a {@code List<Element>} in {@link JPAXMLOverriddenAnnotationReader},
 * with the following constraints:
 * <ul>
 *     <li>Preserve type safety</li>
 *     <li>Only create lists if we actually have elements (most lists should be empty in most cases)</li>
 * </ul>
 */
public final class PropertyMappingElementCollector {
	public static final Function<JaxbPersistentAttribute, String> PERSISTENT_ATTRIBUTE_NAME = JaxbPersistentAttribute::getName;
	public static final Function<JaxbTransientImpl, String> JAXB_TRANSIENT_NAME = JaxbTransientImpl::getName;
	static final Function<JaxbLifecycleCallback, String> LIFECYCLE_CALLBACK_NAME = JaxbLifecycleCallback::getMethodName;

	private final String propertyName;

	private List<JaxbIdImpl> id;
	private List<JaxbEmbeddedIdImpl> embeddedId;
	private List<JaxbVersionImpl> version;
	private List<JaxbBasicImpl> basic;
	private List<JaxbEmbeddedImpl> embedded;
	private List<JaxbOneToOneImpl> oneToOne;
	private List<JaxbManyToOneImpl> manyToOne;
	private List<JaxbElementCollectionImpl> elementCollection;
	private List<JaxbOneToManyImpl> oneToMany;
	private List<JaxbManyToManyImpl> manyToMany;
	private List<JaxbTransientImpl> _transient;

	private List<JaxbPrePersistImpl> prePersist;
	private List<JaxbPostPersistImpl> postPersist;
	private List<JaxbPreRemoveImpl> preRemove;
	private List<JaxbPostRemoveImpl> postRemove;
	private List<JaxbPreUpdateImpl> preUpdate;
	private List<JaxbPostUpdateImpl> postUpdate;
	private List<JaxbPostLoadImpl> postLoad;

	public PropertyMappingElementCollector(String propertyName) {
		this.propertyName = propertyName;
	}

	public boolean isEmpty() {
		return allNullOrEmpty( id, embeddedId, basic, version, manyToOne, oneToMany, oneToOne, manyToMany,
				elementCollection, embedded, _transient,
				prePersist, postPersist, preRemove, postRemove, preUpdate, postUpdate, postLoad );
	}

	private boolean allNullOrEmpty(List<?>... lists) {
		for ( List<?> list : lists ) {
			if ( list != null && !list.isEmpty() ) {
				return false;
			}
		}
		return true;
	}

	private <T> List<T> defaultToEmpty(List<T> list) {
		return list == null ? Collections.emptyList() : list;
	}

	public void collectPersistentAttributesIfMatching(JaxbAttributesContainer container) {
		if ( container instanceof JaxbAttributesContainerImpl jaxbAttributes ) {
			id = collectIfMatching( id, jaxbAttributes.getIdAttributes(), PERSISTENT_ATTRIBUTE_NAME );
			embeddedId = collectIfMatching( embeddedId, jaxbAttributes.getEmbeddedIdAttribute(), PERSISTENT_ATTRIBUTE_NAME );
			version = collectIfMatching( version, jaxbAttributes.getVersion(), PERSISTENT_ATTRIBUTE_NAME );
		}
		basic = collectIfMatching( basic, container.getBasicAttributes(), PERSISTENT_ATTRIBUTE_NAME );
		manyToOne = collectIfMatching( manyToOne, container.getManyToOneAttributes(), PERSISTENT_ATTRIBUTE_NAME );
		oneToMany = collectIfMatching( oneToMany, container.getOneToManyAttributes(), PERSISTENT_ATTRIBUTE_NAME );
		oneToOne = collectIfMatching( oneToOne, container.getOneToOneAttributes(), PERSISTENT_ATTRIBUTE_NAME );
		manyToMany = collectIfMatching( manyToMany, container.getManyToManyAttributes(), PERSISTENT_ATTRIBUTE_NAME );
		elementCollection = collectIfMatching( elementCollection, container.getElementCollectionAttributes(), PERSISTENT_ATTRIBUTE_NAME );
		embedded = collectIfMatching( embedded, container.getEmbeddedAttributes(), PERSISTENT_ATTRIBUTE_NAME );
		_transient = collectIfMatching( _transient, container.getTransients(), JAXB_TRANSIENT_NAME );
	}

	public void collectLifecycleCallbacksIfMatching(JaxbLifecycleCallbackContainer container) {
		prePersist = collectIfMatching( prePersist, container.getPrePersist(), LIFECYCLE_CALLBACK_NAME );
		postPersist = collectIfMatching( postPersist, container.getPostPersist(), LIFECYCLE_CALLBACK_NAME );
		preRemove = collectIfMatching( preRemove, container.getPreRemove(), LIFECYCLE_CALLBACK_NAME );
		postRemove = collectIfMatching( postRemove, container.getPostRemove(), LIFECYCLE_CALLBACK_NAME );
		preUpdate = collectIfMatching( preUpdate, container.getPreUpdate(), LIFECYCLE_CALLBACK_NAME );
		postUpdate = collectIfMatching( postUpdate, container.getPostUpdate(), LIFECYCLE_CALLBACK_NAME );
		postLoad = collectIfMatching( postLoad, container.getPostLoad(), LIFECYCLE_CALLBACK_NAME );
	}

	public void collectTenantIdIfMatching(JaxbManagedType managedType) {
		if ( managedType instanceof JaxbEntityImpl ) {
			JaxbTenantIdImpl tenantId = ( (JaxbEntityImpl) managedType ).getTenantId();
			basic = collectIfMatching( basic, tenantId, PERSISTENT_ATTRIBUTE_NAME );
		}
	}

	private <T> List<T> collectIfMatching(List<T> collected, List<T> candidates,
			Function<? super T, String> nameGetter) {
		List<T> result = collected;
		for ( T candidate : candidates ) {
			result = collectIfMatching( result, candidate, nameGetter );
		}
		return result;
	}

	private <T> List<T> collectIfMatching(List<T> collected, T candidate, Function<? super T, String> nameGetter) {
		List<T> result = collected;
		if ( candidate != null && propertyName.equals( nameGetter.apply( candidate ) ) ) {
			if ( result == null ) {
				result = new ArrayList<>();
			}
			result.add( candidate );
		}
		return result;
	}

	public List<JaxbIdImpl> getId() {
		return defaultToEmpty( id );
	}

	public List<JaxbEmbeddedIdImpl> getEmbeddedId() {
		return defaultToEmpty( embeddedId );
	}

	public List<JaxbBasicImpl> getBasic() {
		return defaultToEmpty( basic );
	}

	public List<JaxbVersionImpl> getVersion() {
		return defaultToEmpty( version );
	}

	public List<JaxbManyToOneImpl> getManyToOne() {
		return defaultToEmpty( manyToOne );
	}

	public List<JaxbOneToManyImpl> getOneToMany() {
		return defaultToEmpty( oneToMany );
	}

	public List<JaxbOneToOneImpl> getOneToOne() {
		return defaultToEmpty( oneToOne );
	}

	public List<JaxbManyToManyImpl> getManyToMany() {
		return defaultToEmpty( manyToMany );
	}

	public List<JaxbElementCollectionImpl> getElementCollection() {
		return defaultToEmpty( elementCollection );
	}

	public List<JaxbEmbeddedImpl> getEmbedded() {
		return defaultToEmpty( embedded );
	}

	public List<JaxbTransientImpl> getTransient() {
		return defaultToEmpty( _transient );
	}

	public List<JaxbPrePersistImpl> getPrePersist() {
		return defaultToEmpty( prePersist );
	}

	public List<JaxbPostPersistImpl> getPostPersist() {
		return defaultToEmpty( postPersist );
	}

	public List<JaxbPreRemoveImpl> getPreRemove() {
		return defaultToEmpty( preRemove );
	}

	public List<JaxbPostRemoveImpl> getPostRemove() {
		return defaultToEmpty( postRemove );
	}

	public List<JaxbPreUpdateImpl> getPreUpdate() {
		return defaultToEmpty( preUpdate );
	}

	public List<JaxbPostUpdateImpl> getPostUpdate() {
		return defaultToEmpty( postUpdate );
	}

	public List<JaxbPostLoadImpl> getPostLoad() {
		return defaultToEmpty( postLoad );
	}
}
