/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.configuration.internal.metadata;

import java.util.Iterator;
import java.util.Locale;

import org.hibernate.envers.boot.EnversMappingException;
import org.hibernate.envers.configuration.internal.metadata.reader.PropertyAuditingData;
import org.hibernate.envers.internal.EnversMessageLogger;
import org.hibernate.envers.internal.tools.Tools;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;

import org.jboss.logging.Logger;

/**
 * Helper class that provides a way to resolve the {@code mappedBy} attribute for collections.
 *
 * @author Chris Cranford
 */
public class CollectionMappedByResolver {

	private static final EnversMessageLogger LOG = Logger.getMessageLogger(
			EnversMessageLogger.class,
			CollectionMappedByResolver.class.getName()
	);

	public static String resolveMappedBy(Collection collection, PropertyAuditingData propertyAuditingData) {
		final PersistentClass referencedClass = getReferenceCollectionClass( collection );
		final ResolverContext resolverContext = new ResolverContext( collection, propertyAuditingData );
		return getMappedBy( referencedClass, resolverContext );
	}

	public static String resolveMappedBy(Table collectionTable, PersistentClass referencedClass, PropertyAuditingData propertyAuditingData) {
		return getMappedBy( referencedClass, new ResolverContext( collectionTable, propertyAuditingData ) );
	}

	public static boolean isMappedByKey(Collection collection, String mappedBy) {
		final PersistentClass referencedClass = getReferenceCollectionClass( collection );
		if ( referencedClass != null ) {
			final String keyMappedBy = searchMappedByKey( referencedClass, collection );
			return mappedBy.equals( keyMappedBy );
		}
		return false;
	}

	private static String getMappedBy(PersistentClass referencedClass, ResolverContext resolverContext) {
		// If there's an @AuditMappedBy specified, returning it directly.
		final String auditMappedBy = resolverContext.propertyAuditingData.getAuditMappedBy();
		if ( auditMappedBy != null ) {
			return auditMappedBy;
		}

		// searching in referenced class
		String mappedBy = searchMappedBy( referencedClass, resolverContext );

		if ( mappedBy == null ) {
			LOG.debugf(
					"Going to search the mapped by attribute for %s in superclasses of entity: %s",
					resolverContext.propertyAuditingData.getName(),
					referencedClass.getClassName()
			);

			PersistentClass tempClass = referencedClass;
			while ( mappedBy == null && tempClass.getSuperclass() != null ) {
				LOG.debugf( "Searching in superclass: %s", tempClass.getSuperclass().getClassName() );
				mappedBy = searchMappedBy( tempClass.getSuperclass(), resolverContext );
				tempClass = tempClass.getSuperclass();
			}
		}

		if ( mappedBy == null ) {
			throw new EnversMappingException(
					String.format(
							Locale.ENGLISH,
							"Unable to read mapped by attribute for %s in %s!",
							resolverContext.propertyAuditingData.getName(),
							referencedClass.getClassName()
					)
			);
		}

		return mappedBy;
	}

	private static String searchMappedBy(PersistentClass persistentClass, ResolverContext resolverContext) {
		if ( resolverContext.getCollection() != null ) {
			return searchMappedBy( persistentClass, resolverContext.getCollection() );
		}
		return searchMappedBy( persistentClass, resolverContext.getTable() );
	}

	private static String searchMappedBy(PersistentClass referencedClass, Collection collectionValue) {
		final Iterator<Property> assocClassProps = referencedClass.getPropertyIterator();
		while ( assocClassProps.hasNext() ) {
			final Property property = assocClassProps.next();

			final Iterator<Selectable> assocClassColumnIterator = property.getValue().getColumnIterator();
			final Iterator<Selectable> collectionKeyColumnIterator = collectionValue.getKey().getColumnIterator();
			if ( Tools.iteratorsContentEqual( assocClassColumnIterator, collectionKeyColumnIterator ) ) {
				return property.getName();
			}
		}
		// HHH-7625
		// Support ToOne relations with mappedBy that point to an @IdClass key property.
		return searchMappedByKey( referencedClass, collectionValue );
	}

	private static String searchMappedBy(PersistentClass referencedClass, Table collectionTable) {
		return searchMappedBy( referencedClass.getPropertyIterator(), collectionTable );
	}

	private static String searchMappedBy(Iterator<Property> properties, Table collectionTable) {
		while ( properties.hasNext() ) {
			final Property property = properties.next();
			if ( property.getValue() instanceof Collection ) {
				// The equality is intentional. We want to find a collection property with the same collection table.
				//noinspection ObjectEquality
				if ( ( (Collection) property.getValue() ).getCollectionTable() == collectionTable ) {
					return property.getName();
				}
			}
			else if ( property.getValue() instanceof Component ) {
				// HHH-12240
				// Should we find an embeddable, we should traverse it as well to see if the collection table
				// happens to be an attribute inside the embeddable rather than directly on the entity.
				final Component component = (Component) property.getValue();

				final String mappedBy = searchMappedBy( component.getPropertyIterator(), collectionTable );
				if ( mappedBy != null ) {
					return property.getName() + "_" + mappedBy;
				}
			}
		}
		return null;
	}

	private static String searchMappedByKey(PersistentClass referencedClass, Collection collectionValue) {
		final Iterator<KeyValue> assocIdClassProps = referencedClass.getKeyClosureIterator();
		while ( assocIdClassProps.hasNext() ) {
			final Value value = assocIdClassProps.next();
			// make sure it's a 'Component' because IdClass is registered as this type.
			if ( value instanceof Component ) {
				final Component component = (Component) value;
				final Iterator<Property> componentPropertyIterator = component.getPropertyIterator();
				while ( componentPropertyIterator.hasNext() ) {
					final Property property = componentPropertyIterator.next();
					final Iterator<Selectable> propertySelectables = property.getValue().getColumnIterator();
					final Iterator<Selectable> collectionSelectables = collectionValue.getKey().getColumnIterator();
					if ( Tools.iteratorsContentEqual( propertySelectables, collectionSelectables ) ) {
						return property.getName();
					}
				}
			}
		}
		return null;
	}

	private static PersistentClass getReferenceCollectionClass(Collection collectionValue) {
		PersistentClass referencedClass = null;
		if ( collectionValue.getElement() instanceof OneToMany ) {
			final OneToMany oneToManyValue = (OneToMany) collectionValue.getElement();
			referencedClass = oneToManyValue.getAssociatedClass();
		}
		else if ( collectionValue.getElement() instanceof ManyToOne ) {
			// Case for bi-directional relation with @JoinTable on the owning @ManyToOne side.
			final ManyToOne manyToOneValue = (ManyToOne) collectionValue.getElement();
			referencedClass = manyToOneValue.getMetadata().getEntityBinding( manyToOneValue.getReferencedEntityName() );
		}
		return referencedClass;
	}

	private static class ResolverContext {
		private final Collection collection;
		private final PropertyAuditingData propertyAuditingData;
		private final Table table;

		public ResolverContext(Collection collection, PropertyAuditingData propertyAuditingData) {
			this.collection = collection;
			this.propertyAuditingData = propertyAuditingData;
			this.table = null;
		}

		public ResolverContext(Table table, PropertyAuditingData propertyAuditingData) {
			this.table = table;
			this.propertyAuditingData = propertyAuditingData;
			this.collection = null;
		}

		public Collection getCollection() {
			return collection;
		}

		public Table getTable() {
			return table;
		}
	}    
}
