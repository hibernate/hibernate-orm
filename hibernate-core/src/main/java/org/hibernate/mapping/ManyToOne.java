/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.hibernate.HibernateError;
import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * A many-to-one association mapping
 * @author Gavin King
 */
public class ManyToOne extends ToOne {
	private boolean ignoreNotFound;
	private boolean isLogicalOneToOne;

	/**
	 * @deprecated since 6.0, use {@link #ManyToOne(MetadataBuildingContext, MappedTable)}
	 */
	@Deprecated
	public ManyToOne(MetadataBuildingContext metadata, Table table) {
		super( metadata, table );
	}

	public ManyToOne(MetadataBuildingContext metadata, MappedTable table) {
		super( metadata, table );
	}

	@Override
	protected void setTypeDescriptorResolver(Column column) {
		column.setTypeDescriptorResolver( new ManyToOneTypeDescriptorResolver( columns.size() - 1 ) );
	}

	public class ManyToOneTypeDescriptorResolver implements TypeDescriptorResolver {
		private int index;

		public ManyToOneTypeDescriptorResolver(int index) {
			this.index = index;
		}

		@Override
		public SqlTypeDescriptor resolveSqlTypeDescriptor() {
			final PersistentClass referencedPersistentClass = getMetadataBuildingContext()
					.getMetadataCollector()
					.getEntityBinding( getReferencedEntityName() );
			if ( referenceToPrimaryKey || referencedPropertyName == null ) {
				return ( (Column) referencedPersistentClass.getIdentifier()
						.getMappedColumns()
						.get( index ) ).getSqlTypeDescriptor();
			}
			else {
				final Property referencedProperty = referencedPersistentClass.getReferencedProperty(
						getReferencedPropertyName() );
				return ( (Column) referencedProperty.getValue()
						.getMappedColumns()
						.get( index ) ).getSqlTypeDescriptor();
			}
		}

		@Override
		public JavaTypeDescriptor resolveJavaTypeDescriptor() {
			return getJavaTypeDescriptor();
		}
	}

	private ForeignKey foreignKey;

	@Override
	public ForeignKey getForeignKey() {
		return foreignKey;
	}

	public ForeignKey createForeignKey() throws MappingException {
		if ( foreignKey == null ) {
			// the case of a foreign key to something other than the pk is handled in createPropertyRefConstraints
			if ( referencedPropertyName == null ) {
				foreignKey = getMappedTable().createForeignKey(
						getForeignKeyName(),
						getConstraintColumns(),
						getReferencedEntityName(),
						getForeignKeyDefinition(),
						null
				);
				if ( hasFormula() ) {
					foreignKey.disableCreation();
				}
			}
		}

		return foreignKey;
	}

	public ForeignKey createPropertyRefConstraints(Map persistentClasses) {
		if ( foreignKey != null ) {
			return foreignKey;
		}

		if ( referencedPropertyName == null ) {
			throw new HibernateError( "#createForeignKey should have created ForeignKey, but none was found" );
		}

		final PersistentClass pc = (PersistentClass) persistentClasses.get( getReferencedEntityName() );
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

		ForeignKey fk = getMappedTable().createForeignKey(
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
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		final PersistentClass referencedPersistentClass = getMetadataBuildingContext()
				.getMetadataCollector()
				.getEntityBinding( getReferencedEntityName() );
		if ( referenceToPrimaryKey || referencedPropertyName == null ) {
			return referencedPersistentClass.getIdentifier().getJavaTypeDescriptor();
		}
		else {
			return referencedPersistentClass.getReferencedProperty( getReferencedPropertyName() )
					.getValue()
					.getJavaTypeDescriptor();
		}
	}
}
