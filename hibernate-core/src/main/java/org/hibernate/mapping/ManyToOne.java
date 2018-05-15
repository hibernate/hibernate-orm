/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.Iterator;
import java.util.Locale;

import org.hibernate.HibernateError;
import org.hibernate.MappingException;
import org.hibernate.boot.model.domain.JavaTypeMapping;
import org.hibernate.boot.model.domain.NotYetResolvedException;
import org.hibernate.boot.model.domain.ResolutionContext;
import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.boot.model.relational.MappedForeignKey;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.boot.spi.MetadataBuildingContext;

/**
 * A many-to-one association mapping
 * @author Gavin King
 */
public class ManyToOne extends ToOne {
	private ForeignKey foreignKey;
	private boolean ignoreNotFound;
	private boolean isLogicalOneToOne;

	/**
	 * @deprecated since 6.0, use {@link #ManyToOne(MetadataBuildingContext, MappedTable)}
	 */
	@Deprecated
	public ManyToOne(MetadataBuildingContext metadata, Table table) {
		this( metadata, (MappedTable) table );
	}

	public ManyToOne(MetadataBuildingContext metadata, MappedTable table) {
		super( metadata, table );
		registerResolver( metadata );
	}

	private void registerResolver(MetadataBuildingContext metadata) {
		metadata.getMetadataCollector().registerValueMappingResolver( this::resolve );
	}

	@Override
	public Boolean resolve(ResolutionContext context) {
		try {
			getJavaTypeMapping().getJavaTypeDescriptor();
		}
		catch ( NotYetResolvedException e ) {
			// ignored, we need to re-resolve this later due to dependency.
			return false;
		}

		final MappedForeignKey foreignKey = getForeignKey();

		final Iterator<MappedColumn> targetColumnItr = foreignKey.getTargetColumns().iterator();

		for ( MappedColumn column : foreignKey.getColumns() ) {
			assert targetColumnItr.hasNext();

			final MappedColumn targetColumn = targetColumnItr.next();
			if ( targetColumn.getJavaTypeMapping() == null ) {
				return false;
			}
			column.setJavaTypeMapping( targetColumn.getJavaTypeMapping() );
			column.setSqlTypeDescriptorAccess( targetColumn::getSqlTypeDescriptor );
		}
		assert !targetColumnItr.hasNext();

		return true;
	}

	@Override
	public ForeignKey getForeignKey() {
		if ( foreignKey == null ) {
			throw new MappingException( "ManyToOne is not yet resolved - cannot yet access ForeignKey" );
		}
		return foreignKey;
	}

	public ForeignKey createForeignKey() throws MappingException {
		if ( foreignKey == null ) {
			// the case of a foreign key to something other than the pk is handled in createPropertyRefConstraints
			if ( referencedPropertyName == null ) {
				foreignKey = (ForeignKey) getMappedTable().createForeignKey(
						getForeignKeyName(),
						getConstraintColumns(),
						getReferencedEntityName(),
						getForeignKeyDefinition(),
						null
				);
				if ( hasFormula() || "none".equals(getForeignKeyName()) ) {
					foreignKey.disableCreation();
				}
			}
		}

		return foreignKey;
	}

	public ForeignKey createPropertyRefConstraints(ResolutionContext context) {
		if ( foreignKey != null ) {
			return foreignKey;
		}

		if ( referencedPropertyName == null ) {
			throw new HibernateError( "#createForeignKey should have created ForeignKey, but none was found" );
		}

		final PersistentClass pc = context.getMetadataBuildingContext()
				.getMetadataCollector()
				.getEntityBinding( getReferencedEntityName() );
		final Property property = pc.getReferencedProperty( getReferencedPropertyName() );

		if ( property == null ) {
			throw new MappingException(
					String.format(
							Locale.ROOT,
							"Could not find property `%s` on `%s : cannot create foreign-key (selectable mappings)",
							getReferencedPropertyName(),
							getReferencedEntityName()
					)
			);
		}

		ForeignKey fk = (ForeignKey) getMappedTable().createForeignKey(
				getForeignKeyName(),
				getConstraintColumns(),
				getReferencedEntityName(),
				getForeignKeyDefinition(),
				property.getMappedColumns()
		);
		fk.setCascadeDeleteEnabled( isCascadeDeleteEnabled() );

		if ( !hasFormula() && !"none".equals( getForeignKeyName() ) ) {
			fk.disableCreation();
		}

		return fk;
	}
	
	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}

	public boolean isIgnoreNotFound() {
		return ignoreNotFound;
	}

	public void setIgnoreNotFound(boolean ignoreNotFound) {
		this.ignoreNotFound = ignoreNotFound;
	}

	public void markAsLogicalOneToOne() {
		this.isLogicalOneToOne = true;
	}

	public boolean isLogicalOneToOne() {
		return isLogicalOneToOne;
	}


	@Override
	public JavaTypeMapping getJavaTypeMapping() {
		final PersistentClass referencedPersistentClass = getMetadataBuildingContext()
				.getMetadataCollector()
				.getEntityBinding( getReferencedEntityName() );
		if ( referenceToPrimaryKey || referencedPropertyName == null ) {
			return referencedPersistentClass.getIdentifier().getJavaTypeMapping();
		}
		else {
			return referencedPersistentClass.getReferencedProperty( getReferencedPropertyName() )
					.getValue()
					.getJavaTypeMapping();
		}
	}
}
