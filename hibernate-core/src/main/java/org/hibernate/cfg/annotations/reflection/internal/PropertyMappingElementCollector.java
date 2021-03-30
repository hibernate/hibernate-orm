/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cfg.annotations.reflection.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.hibernate.boot.jaxb.mapping.spi.AttributesContainer;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAttributes;
import org.hibernate.boot.jaxb.mapping.spi.JaxbBasic;
import org.hibernate.boot.jaxb.mapping.spi.JaxbElementCollection;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbedded;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddedId;
import org.hibernate.boot.jaxb.mapping.spi.JaxbId;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManyToMany;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManyToOne;
import org.hibernate.boot.jaxb.mapping.spi.JaxbOneToMany;
import org.hibernate.boot.jaxb.mapping.spi.JaxbOneToOne;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPostLoad;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPostPersist;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPostRemove;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPostUpdate;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPrePersist;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPreRemove;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPreUpdate;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTransient;
import org.hibernate.boot.jaxb.mapping.spi.JaxbVersion;
import org.hibernate.boot.jaxb.mapping.spi.LifecycleCallback;
import org.hibernate.boot.jaxb.mapping.spi.LifecycleCallbackContainer;
import org.hibernate.boot.jaxb.mapping.spi.PersistentAttribute;

/**
 * Reproduces what we used to do with a {@code List<Element>} in {@link JPAXMLOverriddenAnnotationReader},
 * with the following constraints:
 * <ul>
 *     <li>Preserve type safety</li>
 *     <li>Only create lists if we actually have elements (most lists should be empty in most cases)</li>
 * </ul>
 */
final class PropertyMappingElementCollector {
	static final Function<PersistentAttribute, String> PERSISTENT_ATTRIBUTE_NAME = PersistentAttribute::getName;
	static final Function<JaxbTransient, String> JAXB_TRANSIENT_NAME = JaxbTransient::getName;
	static final Function<LifecycleCallback, String> LIFECYCLE_CALLBACK_NAME = LifecycleCallback::getMethodName;

	private final String propertyName;

	private List<JaxbId> id;
	private List<JaxbEmbeddedId> embeddedId;
	private List<JaxbBasic> basic;
	private List<JaxbVersion> version;
	private List<JaxbManyToOne> manyToOne;
	private List<JaxbOneToMany> oneToMany;
	private List<JaxbOneToOne> oneToOne;
	private List<JaxbManyToMany> manyToMany;
	private List<JaxbElementCollection> elementCollection;
	private List<JaxbEmbedded> embedded;
	private List<JaxbTransient> _transient;

	private List<JaxbPrePersist> prePersist;
	private List<JaxbPostPersist> postPersist;
	private List<JaxbPreRemove> preRemove;
	private List<JaxbPostRemove> postRemove;
	private List<JaxbPreUpdate> preUpdate;
	private List<JaxbPostUpdate> postUpdate;
	private List<JaxbPostLoad> postLoad;

	PropertyMappingElementCollector(String propertyName) {
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

	public void collectPersistentAttributesIfMatching(AttributesContainer container) {
		if ( container instanceof JaxbAttributes ) {
			JaxbAttributes jaxbAttributes = (JaxbAttributes) container;
			id = collectIfMatching( id, jaxbAttributes.getId(), PERSISTENT_ATTRIBUTE_NAME );
			embeddedId = collectIfMatching( embeddedId, jaxbAttributes.getEmbeddedId(), PERSISTENT_ATTRIBUTE_NAME );
			version = collectIfMatching( version, jaxbAttributes.getVersion(), PERSISTENT_ATTRIBUTE_NAME );
		}
		basic = collectIfMatching( basic, container.getBasic(), PERSISTENT_ATTRIBUTE_NAME );
		manyToOne = collectIfMatching( manyToOne, container.getManyToOne(), PERSISTENT_ATTRIBUTE_NAME );
		oneToMany = collectIfMatching( oneToMany, container.getOneToMany(), PERSISTENT_ATTRIBUTE_NAME );
		oneToOne = collectIfMatching( oneToOne, container.getOneToOne(), PERSISTENT_ATTRIBUTE_NAME );
		manyToMany = collectIfMatching( manyToMany, container.getManyToMany(), PERSISTENT_ATTRIBUTE_NAME );
		elementCollection = collectIfMatching( elementCollection, container.getElementCollection(), PERSISTENT_ATTRIBUTE_NAME );
		embedded = collectIfMatching( embedded, container.getEmbedded(), PERSISTENT_ATTRIBUTE_NAME );
		_transient = collectIfMatching( _transient, container.getTransient(), JAXB_TRANSIENT_NAME );
	}

	public void collectLifecycleCallbacksIfMatching(LifecycleCallbackContainer container) {
		prePersist = collectIfMatching( prePersist, container.getPrePersist(), LIFECYCLE_CALLBACK_NAME );
		postPersist = collectIfMatching( postPersist, container.getPostPersist(), LIFECYCLE_CALLBACK_NAME );
		preRemove = collectIfMatching( preRemove, container.getPreRemove(), LIFECYCLE_CALLBACK_NAME );
		postRemove = collectIfMatching( postRemove, container.getPostRemove(), LIFECYCLE_CALLBACK_NAME );
		preUpdate = collectIfMatching( preUpdate, container.getPreUpdate(), LIFECYCLE_CALLBACK_NAME );
		postUpdate = collectIfMatching( postUpdate, container.getPostUpdate(), LIFECYCLE_CALLBACK_NAME );
		postLoad = collectIfMatching( postLoad, container.getPostLoad(), LIFECYCLE_CALLBACK_NAME );
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

	public List<JaxbId> getId() {
		return defaultToEmpty( id );
	}

	public List<JaxbEmbeddedId> getEmbeddedId() {
		return defaultToEmpty( embeddedId );
	}

	public List<JaxbBasic> getBasic() {
		return defaultToEmpty( basic );
	}

	public List<JaxbVersion> getVersion() {
		return defaultToEmpty( version );
	}

	public List<JaxbManyToOne> getManyToOne() {
		return defaultToEmpty( manyToOne );
	}

	public List<JaxbOneToMany> getOneToMany() {
		return defaultToEmpty( oneToMany );
	}

	public List<JaxbOneToOne> getOneToOne() {
		return defaultToEmpty( oneToOne );
	}

	public List<JaxbManyToMany> getManyToMany() {
		return defaultToEmpty( manyToMany );
	}

	public List<JaxbElementCollection> getElementCollection() {
		return defaultToEmpty( elementCollection );
	}

	public List<JaxbEmbedded> getEmbedded() {
		return defaultToEmpty( embedded );
	}

	public List<JaxbTransient> getTransient() {
		return defaultToEmpty( _transient );
	}

	public List<JaxbPrePersist> getPrePersist() {
		return defaultToEmpty( prePersist );
	}

	public List<JaxbPostPersist> getPostPersist() {
		return defaultToEmpty( postPersist );
	}

	public List<JaxbPreRemove> getPreRemove() {
		return defaultToEmpty( preRemove );
	}

	public List<JaxbPostRemove> getPostRemove() {
		return defaultToEmpty( postRemove );
	}

	public List<JaxbPreUpdate> getPreUpdate() {
		return defaultToEmpty( preUpdate );
	}

	public List<JaxbPostUpdate> getPostUpdate() {
		return defaultToEmpty( postUpdate );
	}

	public List<JaxbPostLoad> getPostLoad() {
		return defaultToEmpty( postLoad );
	}
}
