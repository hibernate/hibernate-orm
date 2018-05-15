/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.beanvalidation;

import java.lang.annotation.ElementType;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.validation.Path;
import javax.validation.TraversableResolver;

import org.hibernate.Hibernate;
import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEmbedded;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEntity;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentAttributeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;

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
			EntityTypeDescriptor entityDescriptor,
			ConcurrentHashMap<EntityTypeDescriptor, Set<String>> associationsPerEntityDescriptor) {
		this.associations = associationsPerEntityDescriptor.get( entityDescriptor );
		if (this.associations == null) {
			this.associations = new HashSet<>();
			addAssociationsToTheSetForAllProperties( entityDescriptor );
			associationsPerEntityDescriptor.put( entityDescriptor, associations );
		}
	}

	private void addAssociationsToTheSetForAllProperties(ManagedTypeDescriptor<?> managedTypeDescriptor) {
		for ( PersistentAttributeDescriptor<?, ?> attribute : managedTypeDescriptor.getPersistentAttributes() ) {
			addAssociationsToTheSetForOneProperty( attribute );
		}
	}

	private void addAssociationsToTheSetForOneProperty(PersistentAttributeDescriptor attribute) {
		if ( attribute instanceof PluralPersistentAttribute
				|| attribute instanceof SingularPersistentAttributeEntity ) {
			associations.add( attribute.getNavigableRole().getFullPath() );
		}
		else if ( attribute instanceof SingularPersistentAttributeEmbedded ) {
			addAssociationsToTheSetForAllProperties( ( (SingularPersistentAttributeEmbedded) attribute ).getEmbeddedDescriptor() );
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
