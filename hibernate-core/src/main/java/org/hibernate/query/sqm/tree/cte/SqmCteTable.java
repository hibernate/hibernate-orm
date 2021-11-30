/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.cte;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.dialect.temptable.TemporaryTableColumn;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.PostInsertIdentifierGenerator;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.ExplicitColumnDiscriminatorMappingImpl;
import org.hibernate.metamodel.mapping.internal.SingleAttributeIdentifierMapping;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.sql.ast.tree.cte.CteColumn;

/**
 * @author Steve Ebersole
 * @author Christian Beikov
 */
public class SqmCteTable implements Serializable {
	private final String cteName;
	private final List<SqmCteTableColumn> columns;

	private SqmCteTable(String cteName, Function<SqmCteTable, List<SqmCteTableColumn>> columnInitializer) {
		this.cteName = cteName;
		this.columns = columnInitializer.apply( this );
	}

	public SqmCteTable(String cteName, List<SqmCteTableColumn> columns) {
		this.cteName = cteName;
		this.columns = columns;
	}

	public static SqmCteTable createIdTable(String cteName, EntityMappingType entityDescriptor) {
		return new SqmCteTable(
				cteName,
				sqmCteTable -> {
					final int numberOfColumns = entityDescriptor.getIdentifierMapping().getJdbcTypeCount();
					final List<SqmCteTableColumn> columns = new ArrayList<>( numberOfColumns );
					final EntityIdentifierMapping identifierMapping = entityDescriptor.getIdentifierMapping();
					final String idName;
					if ( identifierMapping instanceof SingleAttributeIdentifierMapping ) {
						idName = ( (SingleAttributeIdentifierMapping) identifierMapping ).getAttributeName();
					}
					else {
						idName = "id";
					}
					columns.add( new SqmCteTableColumn( sqmCteTable, idName, identifierMapping ) );
					return columns;
				}
		);
	}

	public static SqmCteTable createEntityTable(String cteName, EntityMappingType entityDescriptor) {
		return new SqmCteTable(
				cteName,
				sqmCteTable -> {
					final int numberOfColumns = entityDescriptor.getIdentifierMapping().getJdbcTypeCount();
					final List<SqmCteTableColumn> columns = new ArrayList<>( numberOfColumns );
					final EntityIdentifierMapping identifierMapping = entityDescriptor.getIdentifierMapping();
					final String idName;
					if ( identifierMapping instanceof SingleAttributeIdentifierMapping ) {
						idName = ( (SingleAttributeIdentifierMapping) identifierMapping ).getAttributeName();
					}
					else {
						idName = "id";
					}
					columns.add( new SqmCteTableColumn( sqmCteTable, idName, identifierMapping ) );

					final EntityDiscriminatorMapping discriminatorMapping = entityDescriptor.getDiscriminatorMapping();
					if ( discriminatorMapping != null && discriminatorMapping.isPhysical() && !discriminatorMapping.isFormula() ) {
						columns.add( new SqmCteTableColumn( sqmCteTable, "class", discriminatorMapping ) );
					}

					// Collect all columns for all entity subtype attributes
					entityDescriptor.visitSubTypeAttributeMappings(
							attribute -> {
								if ( !( attribute instanceof PluralAttributeMapping ) ) {
									columns.add(
											new SqmCteTableColumn(
													sqmCteTable,
													attribute.getAttributeName(),
													attribute
											)
									);
								}
							}
					);
					// We add a special row number column that we can use to identify and join rows
					columns.add(
							new SqmCteTableColumn(
									sqmCteTable,
									"rn_",
									entityDescriptor.getEntityPersister()
											.getFactory()
											.getTypeConfiguration()
											.getBasicTypeForJavaType( Integer.class )
							)
					);
					return columns;
				}
		);
	}

	public String getCteName() {
		return cteName;
	}

	public List<SqmCteTableColumn> getColumns() {
		return columns;
	}

	public void visitColumns(Consumer<SqmCteTableColumn> columnConsumer) {
		for ( int i = 0; i < columns.size(); i++ ) {
			columnConsumer.accept( columns.get( i ) );
		}
	}
}
