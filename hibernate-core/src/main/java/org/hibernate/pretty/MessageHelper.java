/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.pretty;

import org.hibernate.Internal;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * MessageHelper methods for rendering log messages relating to managed
 * entities and collections typically used in log statements and exception
 * messages.
 *
 * @author Max Andersen, Gavin King
 */
@Internal
public final class MessageHelper {

	private MessageHelper() {
	}


	// entities ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Generate an info message string relating to a particular entity,
	 * based on the given entityName and id.
	 *
	 * @param entityName The defined entity name.
	 * @param id The entity id value.
	 * @return An info string, in the form [FooBar#1].
	 */
	public static String infoString(@Nullable String entityName, @Nullable Object id) {
		final StringBuilder info = new StringBuilder();
		info.append( '[' );
		if ( entityName == null ) {
			info.append( "unknown entity name" );
		}
		else {
			info.append( entityName );
		}

		if ( id == null ) {
			info.append( " with null id" );
		}
		else {
			info.append( " with id '" ).append( id ).append( "'" );
		}
		info.append( ']' );
		return info.toString();
	}

	/**
	 * Generate an info message string relating to a particular entity.
	 *
	 * @param persister The persister for the entity
	 * @param id The entity id value
	 * @param factory The session factory - Could be null!
	 * @return An info string, in the form [FooBar#1]
	 */
	public static String infoString(
			@Nullable EntityPersister persister,
			@Nullable Object id,
			@Nullable SessionFactoryImplementor factory) {
		final StringBuilder info = new StringBuilder();
		info.append( '[' );
		Type idType;
		if( persister == null ) {
			info.append( "unknown entity" );
			idType = null;
		}
		else {
			info.append( persister.getEntityName() );
			idType = persister.getIdentifierType();
		}

		if ( id == null ) {
			info.append( " with null id" );
		}
		else {
			info.append( " with id '" );
			if ( idType == null ) {
				info.append( id );
			}
			else if ( factory != null ) {
				info.append( idType.toLoggableString( id, factory ) );
			}
			else {
				info.append( "<not loggable>" );
			}
			info.append( "'" );
		}
		info.append( ']' );

		return info.toString();

	}

	/**
	 * Generate an info message string relating to a particular entity,.
	 *
	 * @param persister The persister for the entity
	 * @param id The entity id value
	 * @param identifierType The entity identifier type mapping
	 * @param factory The session factory
	 * @return An info string, in the form [FooBar#1]
	 */
	public static String infoString(
			@Nullable EntityPersister persister,
			@Nullable Object id,
			Type identifierType,
			SessionFactoryImplementor factory) {
		final StringBuilder info = new StringBuilder();
		info.append( '[' );
		if( persister == null ) {
			info.append( "unknown entity" );
		}
		else {
			info.append( persister.getEntityName() );
		}

		if ( id == null ) {
			info.append( " with null id" );
		}
		else {
			info.append( " with id '" ).append( identifierType.toLoggableString( id, factory ) ).append( "'" );
		}
		info.append( ']' );

		return info.toString();
	}

	/**
	 * Generate an info message string relating to a series of entities.
	 *
	 * @param persister The persister for the entities
	 * @param ids The entity id values
	 * @param factory The session factory
	 * @return An info string, in the form [FooBar#&lt;1,2,3&gt;]
	 */
	public static String infoString(
			@Nullable EntityPersister persister,
			Object[] ids,
			SessionFactoryImplementor factory) {
		final StringBuilder info = new StringBuilder();
		info.append( '[' );
		if ( persister == null ) {
			info.append( "unknown entity" );
		}
		else {
			info.append( persister.getEntityName() );
			info.append( " with ids " );
			for ( int i=0; i<ids.length; i++ ) {
				info.append( "'" )
					.append( persister.getIdentifierType().toLoggableString( ids[i], factory ) )
					.append( "'" );
				if ( i < ids.length-1 ) {
					info.append( ", " );
				}
			}
		}
		info.append( ']' );
		return info.toString();

	}

	/**
	 * Generate an info message string relating to given entity persister.
	 *
	 * @param persister The persister.
	 * @return An info string, in the form [FooBar]
	 */
	public static String infoString(@Nullable EntityPersister persister) {
		final StringBuilder info = new StringBuilder();
		info.append( '[' );
		if ( persister == null ) {
			info.append( "unknown entity" );
		}
		else {
			info.append( persister.getEntityName() );
		}
		info.append( ']' );
		return info.toString();
	}

	/**
	 * Generate an info message string relating to a given property value
	 * for an entity.
	 *
	 * @param entityName The entity name
	 * @param propertyName The name of the property
	 * @param key The property value.
	 * @return An info string, in the form [Foo.bars#1]
	 */
	public static String infoString(String entityName, String propertyName, @Nullable Object key) {
		final StringBuilder info = new StringBuilder()
				.append( '[' )
				.append( entityName )
				.append( '.' )
				.append( propertyName );

		if ( key == null ) {
			info.append( " with null owner id" );
		}
		else {
			info.append( " with owner id '" ).append( key ).append( "'" );
		}
		info.append( ']' );
		return info.toString();
	}


	// collections ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Generate an info message string relating to a particular managed
	 * collection.  Attempts to intelligently handle property-refs issues
	 * where the collection key is not the same as the owner key.
	 *
	 * @param persister The persister for the collection
	 * @param collection The collection itself
	 * @param collectionKey The collection key
	 * @param session The session
	 * @return An info string, in the form [Foo.bars#1]
	 */
	public static String collectionInfoString(
			@Nullable CollectionPersister persister,
			@Nullable PersistentCollection<?> collection,
			Object collectionKey,
			SharedSessionContractImplementor session ) {
		final StringBuilder info = new StringBuilder();
		info.append( '[' );
		if ( persister == null ) {
			info.append( "unreferenced collection" );
		}
		else {
			info.append( persister.getRole() );
			final Type ownerIdentifierType =
					persister.getOwnerEntityPersister().getIdentifierType();
			final Object ownerKey;
			// TODO: Is it redundant to attempt to use the collectionKey,
			// or is always using the owner id sufficient?
			if ( collectionKey.getClass().isAssignableFrom(
					ownerIdentifierType.getReturnedClass() ) ) {
				ownerKey = collectionKey;
			}
			else {
				final Object collectionOwner = collection == null ? null
						: collection.getOwner();
				final EntityEntry entry = collectionOwner == null ? null
						: session.getPersistenceContextInternal().getEntry( collectionOwner );
				ownerKey = entry == null ? null : entry.getId();
			}
			info.append( " with owner id '" )
					.append( ownerIdentifierType.toLoggableString( ownerKey, session.getFactory() ) )
					.append( "'" );
		}
		info.append( ']' );
		return info.toString();
	}

	/**
	 * Generate an info message string relating to a series of managed
	 * collections.
	 *
	 * @param persister The persister for the collections
	 * @param ids The id values of the owners
	 * @param factory The session factory
	 * @return An info string, in the form [Foo.bars#&lt;1,2,3&gt;]
	 */
	public static String collectionInfoString(
			@Nullable CollectionPersister persister,
			Object[] ids,
			SessionFactoryImplementor factory) {
		final StringBuilder info = new StringBuilder();
		info.append( '[' );
		if ( persister == null ) {
			info.append( "unreferenced collection" );
		}
		else {
			info.append( persister.getRole() );
			info.append( " with owner ids " );
			for ( int i = 0; i < ids.length; i++ ) {
				info.append( "'" );
				addIdToCollectionInfoString( persister, ids[i], factory, info );
				info.append( "'" );
				if ( i < ids.length-1 ) {
					info.append( ", " );
				}
			}
		}
		info.append( ']' );
		return info.toString();
	}

	/**
	 * Generate an info message string relating to a particular managed
	 * collection.
	 *
	 * @param persister The persister for the collection
	 * @param id The id value of the owner
	 * @param factory The session factory
	 * @return An info string, in the form [Foo.bars#1]
	 */
	public static String collectionInfoString(
			@Nullable CollectionPersister persister,
			@Nullable Object id,
			SessionFactoryImplementor factory) {
		final StringBuilder info = new StringBuilder();
		info.append( '[' );
		if ( persister == null ) {
			info.append( "unreferenced collection" );
		}
		else {
			info.append( persister.getRole() );
			if ( id == null ) {
				info.append( " with null owner id" );
			}
			else {
				info.append( " with owner id '" );
				addIdToCollectionInfoString( persister, id, factory, info );
				info.append( "'" );
			}
		}
		info.append( ']' );
		return info.toString();
	}

	private static void addIdToCollectionInfoString(
			CollectionPersister persister,
			Object id,
			SessionFactoryImplementor factory,
			StringBuilder s ) {
		// Need to use the identifier type of the collection owner
		// since the incoming value is actually the owner's id.
		// Using the collection's key type causes problems with
		// property-ref keys.
		// Also need to check that the expected identifier type matches
		// the given ID.  Due to property-ref keys, the collection key
		// may not be the owner key.
		final Type ownerIdentifierType =
				persister.getOwnerEntityPersister().getIdentifierType();
		if ( id.getClass().isAssignableFrom(
				ownerIdentifierType.getReturnedClass() ) ) {
			s.append( ownerIdentifierType.toLoggableString( id, factory ) );
		}
		else {
			// TODO: This is a crappy backup if a property-ref is used.
			// If the reference is an object w/o toString(), this isn't going to work.
			s.append( id );
		}
	}

	/**
	 * Generate an info message string relating to a particular managed
	 * collection.
	 *
	 * @param role The role-name of the collection
	 * @param id The id value of the owner
	 * @return An info string, in the form [Foo.bars#1]
	 */
	public static String collectionInfoString(@Nullable String role, @Nullable Object id) {
		final StringBuilder info = new StringBuilder();
		info.append( '[' );
		if( role == null ) {
			info.append( "unreferenced collection" );
		}
		else {
			info.append( role );
			if ( id == null ) {
				info.append( " with null owner id" );
			}
			else {
				info.append( " with owner id '" ).append( id ).append( "'" );
			}
		}
		info.append( ']' );
		return info.toString();
	}

}
