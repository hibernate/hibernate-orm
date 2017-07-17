/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.beanvalidation;

import java.lang.annotation.ElementType;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.validation.Path;
import javax.validation.TraversableResolver;

import org.hibernate.Hibernate;
import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEmbedded;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentAttribute;
import org.hibernate.type.descriptor.java.internal.AnyTypeJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.CollectionJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.EmbeddableJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;
/**
 * Use Hibernate metadata to ignore cascade on entities.
 * cascade on embeddable objects or collection of embeddable objects are accepted
 *
 * Also use Hibernate's native isInitialized method call.
 * 
 * @author Emmanuel Bernard
 */
public class HibernateTraversableResolver implements TraversableResolver {
	private Set<String> associations;

	public HibernateTraversableResolver(
			EntityDescriptor persister,
			ConcurrentHashMap<EntityDescriptor, Set<String>> associationsPerEntityPersister,
			SessionFactoryImplementor factory) {
		this.associations = associationsPerEntityPersister.get( persister );
		if (this.associations == null) {
			this.associations = new HashSet<>();
			addAssociationsToTheSetForAllProperties( persister.getAttributesByName() );
			associationsPerEntityPersister.put( persister, associations );
		}
	}

	private void addAssociationsToTheSetForAllProperties(Map<String,PersistentAttribute> attributesByName) {
		for(String attributeName : attributesByName.keySet()){
			addAssociationsToTheSetForOneProperty(attributeName, attributesByName.get( attributeName ));
		}
	}

	private void addAssociationsToTheSetForOneProperty(String name, PersistentAttribute attribute) {
		final org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor javaTypeDescriptor = attribute.getJavaTypeDescriptor();
		if ( javaTypeDescriptor instanceof CollectionJavaDescriptor || javaTypeDescriptor instanceof EntityJavaDescriptor || javaTypeDescriptor instanceof AnyTypeJavaDescriptor ) {
			associations.add( attribute.getNavigableRole().getFullPath() );
		}
		else if ( javaTypeDescriptor instanceof EmbeddableJavaDescriptor ) {
			final Map attributesByName = ( (SingularPersistentAttributeEmbedded) attribute ).getEmbeddedDescriptor()
					.getAttributesByName();
			addAssociationsToTheSetForAllProperties( attributesByName );
		}
	}

	private String getStringBasedPath(Path.Node traversableProperty, Path pathToTraversableObject) {
		StringBuilder path = new StringBuilder( );
		for ( Path.Node node : pathToTraversableObject ) {
			if (node.getName() != null) {
				path.append( node.getName() ).append( "." );
			}
		}
		if ( traversableProperty.getName() == null ) {
			throw new AssertionFailure(
					"TraversableResolver being passed a traversableProperty with null name. pathToTraversableObject: "
							+ path.toString() );
		}
		path.append( traversableProperty.getName() );

		return path.toString();
	}

	public boolean isReachable(Object traversableObject,
			Path.Node traversableProperty,
			Class<?> rootBeanType,
			Path pathToTraversableObject,
			ElementType elementType) {
		//lazy, don't load
		return Hibernate.isInitialized( traversableObject )
				&& Hibernate.isPropertyInitialized( traversableObject, traversableProperty.getName() );
	}

	public boolean isCascadable(Object traversableObject,
			Path.Node traversableProperty,
			Class<?> rootBeanType,
			Path pathToTraversableObject,
			ElementType elementType) {
		String path = getStringBasedPath( traversableProperty, pathToTraversableObject );
		return ! associations.contains(path);
	}
}
