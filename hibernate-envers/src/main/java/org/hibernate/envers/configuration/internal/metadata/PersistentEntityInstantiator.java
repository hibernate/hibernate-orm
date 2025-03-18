/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.configuration.internal.metadata;

import java.lang.reflect.Constructor;
import java.util.Objects;

import org.hibernate.envers.boot.EnversMappingException;
import org.hibernate.envers.boot.model.Column;
import org.hibernate.envers.boot.model.DiscriminatorPersistentEntity;
import org.hibernate.envers.boot.model.DiscriminatorType;
import org.hibernate.envers.boot.model.JoinedSubclassPersistentEntity;
import org.hibernate.envers.boot.model.Key;
import org.hibernate.envers.boot.model.PersistentEntity;
import org.hibernate.envers.boot.model.RootPersistentEntity;
import org.hibernate.envers.boot.model.SubclassPersistentEntity;
import org.hibernate.envers.boot.model.UnionSubclassPersistentEntity;
import org.hibernate.envers.configuration.Configuration;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.PrimaryKey;

/**
 * Utility class meant to help facilitate the instantiation of {@link PersistentEntity} implementations.
 *
 * @author Chris Cranford
 */
public class PersistentEntityInstantiator {

	private final Configuration configuration;

	public PersistentEntityInstantiator(Configuration configuration) {
		this.configuration = configuration;
	}

	public PersistentEntity instantiate(PersistentClass persistentClass, AuditTableData auditTableData) {
		Objects.requireNonNull( persistentClass );
		Objects.requireNonNull( auditTableData );

		final InheritanceType inheritanceType = InheritanceType.get( persistentClass );
		switch ( inheritanceType ) {
			case NONE:
				return root( persistentClass, auditTableData );
			case SINGLE:
				return discriminator( persistentClass, auditTableData );
			case JOINED:
				return joined( persistentClass, auditTableData );
			case TABLE_PER_CLASS:
				return union( persistentClass, auditTableData );
			default:
				throw new EnversMappingException( "Unknown inheritance type: " + inheritanceType.name() );
		}
	}

	private PersistentEntity root(PersistentClass persistentClass, AuditTableData auditTableData) {
		RootPersistentEntity entity = instantiate( RootPersistentEntity.class, persistentClass, auditTableData );
		entity.setDiscriminatorValue( persistentClass.getDiscriminatorValue() );

		if ( persistentClass.getDiscriminator() != null ) {
			final DiscriminatorType discriminator = new DiscriminatorType( persistentClass.getDiscriminator() );
			entity.setDiscriminator( discriminator );
		}

		return entity;
	}

	private SubclassPersistentEntity discriminator(PersistentClass persistentClass, AuditTableData auditTableData) {
		DiscriminatorPersistentEntity entity = instantiate( DiscriminatorPersistentEntity.class, persistentClass, auditTableData );
		entity.setExtends( getParentAuditEntityName( getParentEntityName( persistentClass ) ) );
		entity.setDiscriminatorValue( persistentClass.getDiscriminatorValue() );

		return entity;
	}

	private SubclassPersistentEntity joined(PersistentClass persistentClass, AuditTableData auditTableData) {
		JoinedSubclassPersistentEntity entity = instantiate( JoinedSubclassPersistentEntity.class, persistentClass, auditTableData );
		entity.setExtends( getParentAuditEntityName( getParentEntityName( persistentClass ) ) );
		entity.setDiscriminatorValue( persistentClass.getDiscriminatorValue() );

		final Key key = new Key();
		entity.setKey( key );

		// Adding the "key" element with all id columns
		final PrimaryKey primaryKey = persistentClass.getTable().getPrimaryKey();
		primaryKey.getColumns().forEach( column -> key.addColumn( Column.from( column ) ) );

		// the revision number column, read the revision info relation mapping
		configuration.getRevisionInfo().getRevisionInfoRelationMapping().getColumns().forEach( key::addColumn );

		return entity;
	}

	private SubclassPersistentEntity union(PersistentClass persistentClass, AuditTableData auditTableData) {
		UnionSubclassPersistentEntity entity = instantiate( UnionSubclassPersistentEntity.class, persistentClass, auditTableData );
		entity.setExtends( getParentAuditEntityName( getParentEntityName( persistentClass ) ) );

		return entity;
	}

	private <T> T instantiate(Class<T> type, PersistentClass persistentClass, AuditTableData auditTableData) {
		try {
			Constructor<T> constructor = type.getDeclaredConstructor( AuditTableData.class, PersistentClass.class );
			return constructor.newInstance( auditTableData, persistentClass );
		}
		catch (Exception e) {
			throw new EnversMappingException( "Cannot create entity of type " + type.getName() );
		}
	}

	private String getParentEntityName(PersistentClass persistentClass) {
		return persistentClass.getSuperclass().getEntityName();
	}

	private String getParentAuditEntityName(String parentEntityName) {
		return configuration.getAuditEntityName( parentEntityName );
	}
}
