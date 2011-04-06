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

import org.dom4j.Element;

import org.hibernate.MappingException;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.metamodel.binding.SimpleAttributeBinding;
import org.hibernate.metamodel.relational.Size;
import org.hibernate.metamodel.source.util.DomHelper;

/**
 * @author Gail Badner
 */
public class HbmColumnRelationalState extends HbmRelationalState implements SimpleAttributeBinding.ColumnRelationalState {
	private final HbmSimpleValueRelationalStateContainer container;

	/* package-protected */
	HbmColumnRelationalState(Element columnElement,
							HbmSimpleValueRelationalStateContainer container) {
		super( columnElement );
		this.container = container;
	}

	public NamingStrategy getNamingStrategy() {
		return container.getNamingStrategy();
	}
	public String getExplicitColumnName() {
		return getElement().attributeValue( "name" );
	}
	public Size getSize() {
		// TODO: should this set defaults if length, scale, precision is not specified?
		Size size = new Size();
		org.dom4j.Attribute lengthNode = getElement().attribute( "length" );
		if ( lengthNode != null ) {
			size.setLength( Integer.parseInt( lengthNode.getValue() ) );
		}
		org.dom4j.Attribute scaleNode = getElement().attribute( "scale" );
		if ( scaleNode != null ) {
			size.setScale( Integer.parseInt( scaleNode.getValue() ) );
		}
		org.dom4j.Attribute precisionNode = getElement().attribute( "precision" );
		if ( precisionNode != null ) {
			size.setPrecision( Integer.parseInt( precisionNode.getValue() ) );
		}
		// TODO: is there an attribute for lobMultiplier?
		return size;
	}
	public boolean isNullable() {
		return ! DomHelper.extractBooleanAttributeValue( getElement(), "not-null", false );
	}

	public boolean isUnique() {
		return ! DomHelper.extractBooleanAttributeValue( getElement(), "unique", false );
	}

	public String getCheckCondition() {
		return getElement().attributeValue( "check" );
	}
	public String getDefault() {
		return getElement().attributeValue( "default" );
	}
	public String getSqlType() {
		return getElement().attributeValue( "sql-type" );
	}
	public String getCustomWriteFragment() {
		String customWrite = getElement().attributeValue( "write" );
		if ( customWrite != null && ! customWrite.matches("[^?]*\\?[^?]*") ) {
			throw new MappingException("write expression must contain exactly one value placeholder ('?') character");
		}
		return customWrite;
	}
	public String getCustomReadFragment() {
		return getElement().attributeValue( "read" );
	}
	public String getComment() {
		Element comment = getElement().element( "comment" );
		return comment == null ?
				null :
				comment.getTextTrim();
	}
	public Set<String> getUniqueKeys() {
		Set<String> uniqueKeys = DomHelper.extractUniqueAttributeValueTokens( getElement(), "unique-key", ", " );
		uniqueKeys.addAll( container.getPropertyUniqueKeys() );
		return uniqueKeys;
	}
	public Set<String> getIndexes() {
		Set<String> indexes = DomHelper.extractUniqueAttributeValueTokens( getElement(), "index", ", " );
		indexes.addAll( container.getPropertyIndexes() );
		return indexes;
	}
}
