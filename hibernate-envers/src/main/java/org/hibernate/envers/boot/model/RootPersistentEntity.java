/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.boot.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeIdType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmRootEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSimpleIdType;
import org.hibernate.envers.configuration.internal.metadata.AuditTableData;
import org.hibernate.envers.internal.tools.StringTools;
import org.hibernate.mapping.PersistentClass;

/**
 * A persistent entity mapping that represents the root entity of an entity hierarchy.
 *
 * @author Chris Cranford
 */
public class RootPersistentEntity extends PersistentEntity implements JoinAwarePersistentEntity {

	private final List<Attribute> attributes;
	private final List<Join> joins;

	private Identifier identifier;
	private String className;
	private String entityName;
	private String tableName;
	private String whereClause;
	private DiscriminatorType discriminator;
	private String discriminatorValue;

	public RootPersistentEntity(AuditTableData auditTableData, PersistentClass persistentClass) {
		super( auditTableData, persistentClass );
		this.attributes = new ArrayList<>();
		this.joins = new ArrayList<>();
	}

	public RootPersistentEntity(AuditTableData auditTableData, Class<?> clazz, String entityName, String tableName) {
		super( auditTableData, null );
		this.attributes = new ArrayList<>();
		this.joins = new ArrayList<>();
		this.className = clazz.getName();
		this.entityName = entityName;
		this.tableName = tableName;
	}

	@Override
	public boolean isJoinAware() {
		return true;
	}

	@Override
	public void addAttribute(Attribute attribute) {
		attributes.add( attribute );
	}

	public Identifier getIdentifier() {
		return identifier;
	}

	public void setIdentifier(Identifier identifier) {
		this.identifier = identifier;
	}

	public DiscriminatorType getDiscriminator() {
		return discriminator;
	}

	public void setDiscriminator(DiscriminatorType discriminator) {
		this.discriminator = discriminator;
	}

	public String getDiscriminatorValue() {
		return discriminatorValue;
	}

	public void setDiscriminatorValue(String discriminatorValue) {
		this.discriminatorValue = discriminatorValue;
	}

	public void setWhereClause(String whereClause) {
		this.whereClause = whereClause;
	}

	@Override
	public List<Join> getJoins() {
		return Collections.unmodifiableList( joins );
	}

	@Override
	public void addJoin(Join join) {
		this.joins.add( join );
	}

	@Override
	public void build(JaxbHbmHibernateMapping mapping) {
		mapping.getClazz().add( buildJaxbMapping() );
	}

	private JaxbHbmRootEntityType buildJaxbMapping() {
		final JaxbHbmRootEntityType entity = new JaxbHbmRootEntityType();

		// Set common stuff
		if ( getPersistentClass() != null ) {
			entity.setAbstract(getPersistentClass().isAbstract());
		}

		if ( !StringTools.isEmpty( getAuditTableData().getAuditEntityName() ) ) {
			entity.setEntityName( getAuditTableData().getAuditEntityName() );
		}
		else if ( !StringTools.isEmpty( className ) ) {
			entity.setName( className );
			if ( !StringTools.isEmpty( entityName ) ) {
				entity.setEntityName( entityName );
			}
		}

		if ( !StringTools.isEmpty( getAuditTableData().getAuditTableName() ) ) {
			entity.setTable( getAuditTableData().getAuditTableName() );
		}
		else if ( !StringTools.isEmpty( tableName ) ) {
			entity.setTable( tableName );
		}

		if ( !StringTools.isEmpty( getAuditTableData().getSchema() ) ) {
			entity.setSchema( getAuditTableData().getSchema() );
		}

		if ( !StringTools.isEmpty( getAuditTableData().getCatalog() ) ) {
			entity.setCatalog( getAuditTableData().getCatalog() );
		}

		Object id = identifier.build();
		if (id instanceof JaxbHbmSimpleIdType) {
			entity.setId( (JaxbHbmSimpleIdType) id );
		}
		else if (id instanceof JaxbHbmCompositeIdType) {
			entity.setCompositeId( (JaxbHbmCompositeIdType) id );
		}
		else {
			throw new HibernateException( "Unknown identifier type: " + id.getClass().getName() );
		}

		// Initialize attributes
		for (Attribute attribute : attributes) {
			entity.getAttributes().add( attribute.build() );
		}

		for (Join join : joins) {
			entity.getJoin().add( join.build() );
		}

		if ( !StringTools.isEmpty( whereClause ) ) {
			entity.setWhere( whereClause );
		}

		if ( discriminator != null ) {
			entity.setDiscriminator( discriminator.build() );
		}

		if ( !StringTools.isEmpty( discriminatorValue ) ) {
			entity.setDiscriminatorValue( discriminatorValue );
		}

		return entity;
	}

}
