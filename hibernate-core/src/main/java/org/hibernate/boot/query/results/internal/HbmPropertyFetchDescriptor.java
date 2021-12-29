/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.query.results.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryPropertyReturnType;
import org.hibernate.boot.query.results.BootResultMappingLogging;
import org.hibernate.boot.query.results.HbmFetchDescriptor;
import org.hibernate.boot.query.results.HbmFetchParent;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.EntityCollectionPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.internal.FetchMementoBasicStandard;
import org.hibernate.query.named.FetchMemento;
import org.hibernate.query.named.ResultMemento;
import org.hibernate.query.results.FetchParentMemento;
import org.hibernate.query.results.ResultSetMappingResolutionContext;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.FetchableContainer;

/**
 * @see JaxbHbmNativeQueryPropertyReturnType
 *
 * @author Steve Ebersole
 */
public class HbmPropertyFetchDescriptor implements HbmFetchDescriptor {
	private final HbmFetchParent parent;
	private final String propertyPath;
	private final String[] propertyPathParts;
	private final List<String> columnAliases;

	public HbmPropertyFetchDescriptor(
			JaxbHbmNativeQueryPropertyReturnType hbmPropertyMapping,
			HbmFetchParent parent,
			String registrationName,
			MetadataBuildingContext context) {
		this.parent = parent;
		this.propertyPath = hbmPropertyMapping.getName();
		this.propertyPathParts = propertyPath.split( "\\." );
		this.columnAliases = extractColumnAliases( hbmPropertyMapping, context );

		BootResultMappingLogging.LOGGER.debugf(
				"Creating PropertyFetchDescriptor (%s : %s) for ResultSet mapping - %s",
				parent,
				propertyPath,
				registrationName
		);
	}

	@Override
	public String getFetchablePath() {
		return propertyPath;
	}

	@Override
	public String toString() {
		return "HbmPropertyFetchDescriptor(" + propertyPath + ")";
	}

	private static List<String> extractColumnAliases(
			JaxbHbmNativeQueryPropertyReturnType hbmPropertyMapping,
			MetadataBuildingContext context) {
		if ( hbmPropertyMapping.getColumn() != null ) {
			return Collections.singletonList( hbmPropertyMapping.getColumn() );
		}

		final List<String> columnAliases = new ArrayList<>( hbmPropertyMapping.getReturnColumn().size() );
		hbmPropertyMapping.getReturnColumn().forEach(
				(column) -> columnAliases.add( column.getName() )
		);
		return columnAliases;
	}

	@Override
	public FetchMemento resolve(ResultSetMappingResolutionContext resolutionContext) {
		BootResultMappingLogging.LOGGER.debugf(
				"Resolving HBM PropertyFetchDescriptor into memento - %s : %s",
				parent,
				propertyPath
		);

		final FetchParentMemento fetchParentMemento = parent.resolveParentMemento( resolutionContext );
		final FetchableContainer fetchableContainer = fetchParentMemento.getFetchableContainer();

		String firstPathPart = propertyPathParts[0];
		int subPathStartIndex = 1;

		Fetchable fetchable = null;
		NavigablePath navigablePath = null;

		// some special cases...
		if ( fetchableContainer instanceof PluralAttributeMapping ) {
			final PluralAttributeMapping pluralAttr = (PluralAttributeMapping) fetchableContainer;

			if ( "element".equals( firstPathPart ) ) {
				if ( propertyPathParts.length == 1 ) {
					// this is a reference to the "key" or "referring" side of the collection fk
					return new HbmForeignKeyRefMemento(
							fetchParentMemento.getNavigablePath()
									.append( ForeignKeyDescriptor.PART_NAME )
									.append( ForeignKeyDescriptor.Nature.KEY.getName() ),
							pluralAttr.getKeyDescriptor(),
							pluralAttr.getKeyDescriptor().getKeySide(),
							columnAliases
					);
				}
				else {
					subPathStartIndex = 2;
					firstPathPart = propertyPathParts[1];
					navigablePath = fetchParentMemento.getNavigablePath().append( CollectionPart.Nature.ELEMENT.getName() );
					fetchable = pluralAttr.getElementDescriptor();
				}
			}

			if ( "key".equals( firstPathPart ) ) {
				if ( propertyPathParts.length == 1 ) {
					// this is a reference to the "target" side of the collection fk
					return new HbmForeignKeyRefMemento(
							fetchParentMemento.getNavigablePath()
									.append( ForeignKeyDescriptor.PART_NAME )
									.append( ForeignKeyDescriptor.Nature.TARGET.getName() ),
							pluralAttr.getKeyDescriptor(),
							pluralAttr.getKeyDescriptor().getTargetSide(),
							columnAliases
					);
				}
				else {
					subPathStartIndex = 2;
					firstPathPart = propertyPathParts[1];
					fetchable = pluralAttr.getElementDescriptor();
				}
			}
		}

		if ( fetchable == null ) {
			assert navigablePath == null;
			fetchable = (Fetchable) fetchableContainer.findSubPart( firstPathPart, null );
			navigablePath = fetchParentMemento.getNavigablePath().append( fetchable.getFetchableName() );
		}

		for ( int i = subPathStartIndex; i < propertyPathParts.length; i++ ) {
			if ( !( fetchable instanceof FetchableContainer ) ) {
				throw new MappingException(
						"Non-terminal property path [" + navigablePath.getFullPath()
								+ " did not reference FetchableContainer"
				);
			}
			navigablePath = navigablePath.append( propertyPathParts[ i ] );
			fetchable = (Fetchable) ( (FetchableContainer) fetchable ).findSubPart( propertyPathParts[ i ], null );
		}

		if ( fetchable instanceof ToOneAttributeMapping ) {
			final ToOneAttributeMapping toOne = (ToOneAttributeMapping) fetchable;
			fetchable = (Fetchable) toOne.getForeignKeyDescriptor().getKeyPart();
			navigablePath = navigablePath.append( EntityIdentifierMapping.ROLE_LOCAL_NAME );
		}
		else if ( fetchable instanceof EntityCollectionPart ) {
			final EntityCollectionPart collectionPart = (EntityCollectionPart) fetchable;
			fetchable = (Fetchable) collectionPart.getForeignKeyDescriptor().getKeyPart();
			navigablePath = navigablePath.append( EntityIdentifierMapping.ROLE_LOCAL_NAME );
		}

		return new FetchMementoBasicStandard( navigablePath, (BasicValuedModelPart) fetchable, columnAliases.get( 0 ) );
	}

	@Override
	public ResultMemento asResultMemento(NavigablePath path, ResultSetMappingResolutionContext resolutionContext) {
		throw new UnsupportedOperationException( "PropertyFetchDescriptor cannot be converted to a result" );
	}
}
