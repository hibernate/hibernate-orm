/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.persister.embeddable.spi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.persister.common.internal.CompositeContainer;
import org.hibernate.persister.common.internal.PersisterHelper;
import org.hibernate.persister.common.spi.Attribute;
import org.hibernate.persister.common.spi.AttributeContainer;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.JoinColumnMapping;
import org.hibernate.persister.common.spi.JoinableAttribute;
import org.hibernate.persister.common.spi.JoinableAttributeContainer;
import org.hibernate.persister.common.spi.OrmTypeExporter;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.convert.spi.TableGroupProducer;
import org.hibernate.type.spi.CompositeType;

/**
 * @author Steve Ebersole
 */
public class EmbeddablePersister
		implements OrmTypeExporter, AttributeContainer, JoinableAttributeContainer, CompositeContainer {
	private final CompositeContainer compositeContainer;
	private final String compositeName;
	private final String roleName;
	private final CompositeType ormType;
	private final List<Column> allColumns;

	private final Map<String, Attribute> attributeMap = new HashMap<>();
	private final List<Attribute> attributeList = new ArrayList<>();

	public EmbeddablePersister(
			CompositeContainer compositeContainer,
			String compositeName,
			String roleName,
			CompositeType ormType,
			PersisterCreationContext creationContext,
			List<Column> allColumns) {
		this.compositeContainer = compositeContainer;
		this.compositeName = compositeName;
		this.roleName = roleName;
		this.ormType = ormType;
		this.allColumns = allColumns;

		assert ormType.getPropertyNames().length == ormType.getSubtypes().length;

		int columnSpanStart = 0, columnSpanEnd;

		for ( int i = 0; i < ormType.getPropertyNames().length; i++ ) {
			final String propertyName = ormType.getPropertyNames()[i];
			final org.hibernate.type.spi.Type propertyType = ormType.getSubtypes()[i];

			final int columnSpan = propertyType.getColumnSpan();
			final List<Column> columns = new ArrayList<>();
			columnSpanEnd = columnSpanStart + columnSpan;
			for ( int j = columnSpanStart; j < columnSpanEnd; j++ ) {
				columns.add( allColumns.get( j ) );
			}

			final Attribute attribute = PersisterHelper.INSTANCE.buildAttribute(
					creationContext,
					this,
					propertyName,
					propertyType,
					columns
			);
			attributeMap.put( propertyName, attribute );
			attributeList.add( attribute );

			columnSpanStart = columnSpanEnd;
		}
	}

	public List<Column> collectColumns() {
		return allColumns;
	}

	@Override
	public AttributeContainer getSuperAttributeContainer() {
		// for now we do not support composite inheritance
		return null;
	}

	@Override
	public List<Attribute> getNonIdentifierAttributes() {
		return attributeList.stream().collect( Collectors.toList() );
	}

	@Override
	public Attribute findAttribute(String name) {
		return attributeMap.get( name );
	}

	@Override
	public List<JoinColumnMapping> resolveJoinColumnMappings(Attribute attribute) {
		// todo : associations defined on composites
		throw new NotYetImplementedException(  );
	}

	@Override
	public CompositeType getOrmType() {
		return ormType;
	}

	@Override
	public String asLoggableText() {
		return "EmdeddablePersister(" + roleName + " [" + compositeName + "])";
	}

	@Override
	public TableGroupProducer resolveTableGroupProducer() {
		return compositeContainer.resolveTableGroupProducer();
	}

	@Override
	public boolean canCompositeContainCollections() {
		return compositeContainer.canCompositeContainCollections();
	}

	@Override
	public List<JoinColumnMapping> resolveJoinColumnMappings(JoinableAttribute joinableAttribute) {
		throw new NotYetImplementedException();
	}
}
