/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.metamodel.source.hbm.state.relational;

import java.util.Set;

import org.hibernate.MappingException;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.metamodel.binding.SimpleAttributeBinding;
import org.hibernate.metamodel.relational.Size;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLColumnElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLDiscriminator;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLId;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLProperty;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLTimestamp;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLVersion;
import org.hibernate.metamodel.source.util.MappingHelper;

// TODO: remove duplication after Id, Discriminator, Version, Timestamp, and Property extend a common interface.

/**
 * @author Gail Badner
 */
public class HbmColumnRelationalState implements SimpleAttributeBinding.ColumnRelationalState {
	private final HbmSimpleValueRelationalStateContainer container;
	private final String explicitColumnName;
	private final Size size;
	private final boolean isNullable;
	private final boolean isUnique;
	private final String checkCondition;
	private final String defaultColumnValue;
	private final String sqlType;
	private final String customWrite;
	private final String customRead;
	private final String comment;
	private final Set<String> uniqueKeys;
	private final Set<String> indexes;

	/* package-protected */
	HbmColumnRelationalState(XMLColumnElement columnElement,
							HbmSimpleValueRelationalStateContainer container) {
		this.container = container;
		this.explicitColumnName = columnElement.getName();
		this.size = createSize( columnElement.getLength(), columnElement.getScale(), columnElement.getPrecision() );
		this.isNullable = createNullable( columnElement.getNotNull() );
		this.isUnique = createUnique( columnElement.getUnique() );
		this.checkCondition = columnElement.getCheck();
		this.defaultColumnValue = columnElement.getDefault();
		this.sqlType = columnElement.getSqlType();
		this.customWrite = columnElement.getWrite();
		if ( customWrite != null && ! customWrite.matches("[^?]*\\?[^?]*") ) {
			throw new MappingException("write expression must contain exactly one value placeholder ('?') character");
		}
		this.customRead = columnElement.getRead();
		this.comment = columnElement.getComment() == null ? null : columnElement.getComment().trim();
		this.uniqueKeys = MappingHelper.getStringValueTokens( columnElement.getUniqueKey(), ", " );
		this.uniqueKeys.addAll( container.getPropertyUniqueKeys() );
		this.indexes = MappingHelper.getStringValueTokens( columnElement.getIndex(), ", " );
		this.indexes.addAll( container.getPropertyIndexes() );
	}

	HbmColumnRelationalState(XMLProperty property,
							HbmSimpleValueRelationalStateContainer container) {
		this.container = container;
		this.explicitColumnName = property.getName();
		this.size = createSize( property.getLength(), property.getScale(), property.getPrecision() );
		this.isNullable = createNullable( property.getNotNull() );
		this.isUnique = createUnique( property.getUnique() );
		this.checkCondition = null;
		this.defaultColumnValue = null;
		this.sqlType = null;
		this.customWrite = null;
		this.customRead = null;
		this.comment = null;
		this.uniqueKeys = MappingHelper.getStringValueTokens( property.getUniqueKey(), ", " );
		this.uniqueKeys.addAll( container.getPropertyUniqueKeys() );
		this.indexes = MappingHelper.getStringValueTokens( property.getIndex(), ", " );
		this.indexes.addAll( container.getPropertyIndexes() );
	}

	HbmColumnRelationalState(XMLId id,
							HbmSimpleValueRelationalStateContainer container) {
		if ( id.getColumnElement() != null && ! id.getColumnElement().isEmpty() ) {
			throw new IllegalArgumentException( "This method should not be called with non-empty id.getColumnElement()" );
		}
		this.container = container;
		this.explicitColumnName = id.getName();
		this.size = createSize( id.getLength(), null, null );
		this.isNullable = false;
		this.isUnique = true;
		this.checkCondition = null;
		this.defaultColumnValue = null;
		this.sqlType = null;
		this.customWrite = null;
		this.customRead = null;
		this.comment = null;
		this.uniqueKeys = container.getPropertyUniqueKeys();
		this.indexes = container.getPropertyIndexes();
	}

	HbmColumnRelationalState(XMLDiscriminator discriminator,
							HbmSimpleValueRelationalStateContainer container) {
		if ( discriminator.getColumnElement() != null  ) {
			throw new IllegalArgumentException( "This method should not be called with null discriminator.getColumnElement()" );
		}
		this.container = container;
		this.explicitColumnName = null;
		this.size = createSize( discriminator.getLength(), null, null );
		this.isNullable = false;
		this.isUnique = true;
		this.checkCondition = null;
		this.defaultColumnValue = null;
		this.sqlType = null;
		this.customWrite = null;
		this.customRead = null;
		this.comment = null;
		this.uniqueKeys = container.getPropertyUniqueKeys();
		this.indexes = container.getPropertyIndexes();
	}

	HbmColumnRelationalState(XMLVersion version,
							HbmSimpleValueRelationalStateContainer container) {
		this.container = container;
		this.explicitColumnName = version.getColumn();
		if ( version.getColumnElement() != null && ! version.getColumnElement().isEmpty() ) {
			throw new IllegalArgumentException( "This method should not be called with non-empty version.getColumnElement()" );
		}
		// TODO: should set default
		this.size = new Size();
		this.isNullable = false;
		this.isUnique = false;
		this.checkCondition = null;
		this.defaultColumnValue = null;
		this.sqlType = null; // TODO: figure out the correct setting
		this.customWrite = null;
		this.customRead = null;
		this.comment = null;
		this.uniqueKeys = container.getPropertyUniqueKeys();
		this.indexes = container.getPropertyIndexes();
	}

	HbmColumnRelationalState(XMLTimestamp timestamp,
							HbmSimpleValueRelationalStateContainer container) {
		this.container = container;
		this.explicitColumnName = timestamp.getColumn();
		// TODO: should set default
		this.size = new Size();
		this.isNullable = false;
		this.isUnique = true; // well, it should hopefully be unique...
		this.checkCondition = null;
		this.defaultColumnValue = null;
		this.sqlType = null; // TODO: figure out the correct setting
		this.customWrite = null;
		this.customRead = null;
		this.comment = null;
		this.uniqueKeys = container.getPropertyUniqueKeys();
		this.indexes = container.getPropertyIndexes();
	}

	public NamingStrategy getNamingStrategy() {
		return container.getNamingStrategy();
	}
	public String getExplicitColumnName() {
		return explicitColumnName;
	}
	public Size getSize() {
		return size;
	}
	private static Size createSize(String length, String scale, String precision) {
		// TODO: should this set defaults if length, scale, precision is not specified?
		Size size = new Size();
		if ( length != null ) {
			size.setLength( Integer.parseInt( length ) );
		}
		if ( scale != null ) {
			size.setScale( Integer.parseInt( scale ) );
		}
		if ( precision != null ) {
			size.setPrecision( Integer.parseInt( precision ) );
		}
		// TODO: is there an attribute for lobMultiplier?
		return size;
	}
	public boolean isNullable() {
		return isNullable;
	}

	private static boolean createNullable(String notNullString ) {
		return ! MappingHelper.getBooleanValue( notNullString, false );
	}

	public boolean isUnique() {
		return isUnique;
	}

	private boolean createUnique(String uniqueString) {
		return ! MappingHelper.getBooleanValue( uniqueString, false );
	}

	public String getCheckCondition() {
		return checkCondition;
	}
	public String getDefault() {
		return defaultColumnValue;
	}
	public String getSqlType() {
		return sqlType;
	}
	public String getCustomWriteFragment() {
		return customWrite;
	}
	public String getCustomReadFragment() {
		return customRead;
	}
	public String getComment() {
		return comment;
	}
	public Set<String> getUniqueKeys() {
		return uniqueKeys;
	}
	public Set<String> getIndexes() {
		return indexes;
	}
}
