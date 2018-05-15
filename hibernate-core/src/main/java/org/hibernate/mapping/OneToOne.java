/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;

import org.hibernate.MappingException;
import org.hibernate.boot.model.domain.JavaTypeMapping;
import org.hibernate.boot.model.domain.ResolutionContext;
import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.type.ForeignKeyDirection;

/**
 * A one-to-one association mapping
 * @author Gavin King
 */
public class OneToOne extends ToOne {
	private boolean constrained;
	private ForeignKeyDirection foreignKeyType;
	private KeyValue identifier;
	private String propertyName;
	private String entityName;

	/**
	 * @deprecated since 6.0, use {@link #OneToOne(MetadataBuildingContext, MappedTable, PersistentClass)} instead
	 */
	@Deprecated
	public OneToOne(MetadataBuildingContext metadata, Table table, PersistentClass owner) throws MappingException {
		this( metadata, (MappedTable) table, owner );
	}

	public OneToOne(MetadataBuildingContext metadata, MappedTable table, PersistentClass owner) throws MappingException {
		super( metadata, table );
		this.identifier = owner.getKey();
		this.entityName = owner.getEntityName();

		registerResolver( metadata );
	}

	private void registerResolver(MetadataBuildingContext metadata) {
		metadata.getMetadataCollector().registerValueMappingResolver( this::resolve );
	}

	public String getPropertyName() {
		return propertyName;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName==null ? null : propertyName.intern();
	}
	
	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(String propertyName) {
		this.entityName = entityName==null ? null : entityName.intern();
	}

	private ForeignKey foreignKey;

	@Override
	public ForeignKey getForeignKey() {
		return foreignKey;
	}

	public ForeignKey createForeignKey() throws MappingException {
		if ( constrained ) {
			this.foreignKey = createForeignKeyOfEntity( getReferencedEntityName() );
			if ( this.referencedPropertyName == null ) {
				foreignKey.disableCreation();
			}
		}

		return foreignKey;
	}

	@Override
	public Boolean resolve(ResolutionContext context) {
		getJavaTypeMapping().getJavaTypeDescriptor();

		final ForeignKey foreignKey = getForeignKey();

		if ( foreignKey == null ) {
			// the assumption here is that all `#createForeignKey` calls have
			//		been done and therefore this is not the side that owns the
			//		FK...
			return true;
		}

		// conversely, we assume here that this side does own the FK
		//		so we manage the cross-column config

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

	public java.util.List<MappedColumn> getConstraintColumns() {
		final ArrayList<MappedColumn> list = new ArrayList();
		identifier.getMappedColumns().forEach( o -> list.add( (MappedColumn) o ) );
		return list;
	}
	/**
	 * Returns the constrained.
	 * @return boolean
	 */
	public boolean isConstrained() {
		return constrained;
	}

	/**
	 * Returns the foreignKeyType.
	 * @return AssociationType.ForeignKeyType
	 */
	public ForeignKeyDirection getForeignKeyType() {
		return foreignKeyType;
	}

	/**
	 * Returns the identifier.
	 * @return Value
	 */
	public KeyValue getIdentifier() {
		return identifier;
	}

	/**
	 * Sets the constrained.
	 * @param constrained The constrained to set
	 */
	public void setConstrained(boolean constrained) {
		this.constrained = constrained;
	}

	/**
	 * Sets the foreignKeyType.
	 * @param foreignKeyType The foreignKeyType to set
	 */
	public void setForeignKeyType(ForeignKeyDirection foreignKeyType) {
		this.foreignKeyType = foreignKeyType;
	}

	/**
	 * Sets the identifier.
	 * @param identifier The identifier to set
	 */
	public void setIdentifier(KeyValue identifier) {
		this.identifier = identifier;
	}

	public boolean isNullable() {
		return !constrained;
	}

	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}

	@Override
	public boolean isSame(ToOne other) {
		return other instanceof OneToOne && isSame( (OneToOne) other );
	}

	public boolean isSame(OneToOne other) {
		return super.isSame( other )
				&& Objects.equals( foreignKeyType, other.foreignKeyType )
				&& isSame( identifier, other.identifier )
				&& Objects.equals( propertyName, other.propertyName )
				&& Objects.equals( entityName, other.entityName )
				&& constrained == other.constrained;
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
