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
package org.hibernate.metamodel.spi.binding;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.hibernate.MappingException;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.Schema;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.source.MetaAttributeContext;

/**
 * TODO : this really needs an overhaul...  mainly, get rid of the KeyValueBinding concept...
 *
 * @author Steve Ebersole
 */
public class BasicAttributeBinding
		extends AbstractSingularAttributeBinding
		implements SingularNonAssociationAttributeBinding {

	private final List<RelationalValueBinding> relationalValueBindings;
	private boolean hasDerivedValue;
	private boolean isNullable = true;
	private final PropertyGeneration generation;

	BasicAttributeBinding(
			AttributeBindingContainer container,
			SingularAttribute attribute,
			List<RelationalValueBinding> relationalValueBindings,
			String propertyAccessorName,
			boolean includedInOptimisticLocking,
			boolean lazy,
			MetaAttributeContext metaAttributeContext,
			PropertyGeneration generation) {
		super(
				container,
				attribute,
				propertyAccessorName,
				includedInOptimisticLocking,
				lazy,
				metaAttributeContext
		);
		this.relationalValueBindings = Collections.unmodifiableList( relationalValueBindings );
		for ( RelationalValueBinding relationalValueBinding : relationalValueBindings ) {
			this.hasDerivedValue = this.hasDerivedValue || relationalValueBinding.isDerived();
			this.isNullable = this.isNullable && relationalValueBinding.isNullable();
		}
		this.generation = generation;
	}

	@Override
	public boolean isAssociation() {
		return false;
	}

	public List<RelationalValueBinding> getRelationalValueBindings() {
		return relationalValueBindings;
	}

	@Override
	public boolean hasDerivedValue() {
		return hasDerivedValue;
	}

	@Override
	public boolean isNullable() {
		return isNullable;
	}

	public PropertyGeneration getGeneration() {
		return generation;
	}

	@Override
	public IdentifierGenerator createIdentifierGenerator(
			IdGenerator idGenerator,
			IdentifierGeneratorFactory identifierGeneratorFactory,
			Properties properties) {
		if ( getRelationalValueBindings().size() > 1 ) {
			throw new MappingException(
					"A SimpleAttributeBinding used for an identifier has more than 1 Value: " + getAttribute().getName()
			);
		}

		final RelationalValueBinding relationalValueBinding = getRelationalValueBindings().get( 0 );
		final TableSpecification table = relationalValueBinding.getValue().getTable();
		if ( !Column.class.isInstance( relationalValueBinding.getValue() ) ) {
			throw new MappingException(
					"Cannot create an IdentifierGenerator because the value is not a column: " +
							relationalValueBinding.getValue().toLoggableString()
			);
		}

		Properties params = new Properties();
		params.putAll( properties );

		// use the schema/catalog specified by getValue().getTable() - but note that
		// if the schema/catalog were specified as params, they will already be initialized and
		//will override the values set here (they are in idGenerator.getParameters().)
		Schema schema = table.getSchema();
		if ( schema != null ) {
			if ( schema.getName().getSchema() != null ) {
				params.setProperty( PersistentIdentifierGenerator.SCHEMA, schema.getName().getSchema().getName() );
			}
			if ( schema.getName().getCatalog() != null ) {
				params.setProperty( PersistentIdentifierGenerator.CATALOG, schema.getName().getCatalog().getName() );
			}
		}

		params.setProperty( IdentifierGenerator.ENTITY_NAME, getContainer().seekEntityBinding().getEntity().getName() );

		//init the table here instead of earlier, so that we can get a quoted table name
		//TODO: would it be better to simply pass the qualified table name, instead of
		//      splitting it up into schema/catalog/table names
		String tableName = table.getQualifiedName( identifierGeneratorFactory.getDialect() );
		params.setProperty( PersistentIdentifierGenerator.TABLE, tableName );

		params.setProperty(
				PersistentIdentifierGenerator.PK,
				( (Column) relationalValueBinding.getValue() ).getColumnName().encloseInQuotesIfQuoted(
						identifierGeneratorFactory.getDialect()
				)
		);
		params.setProperty( PersistentIdentifierGenerator.TABLES, tableName );
		params.putAll( idGenerator.getParameters() );
		return identifierGeneratorFactory.createIdentifierGenerator(
				idGenerator.getStrategy(), getHibernateTypeDescriptor().getResolvedTypeMapping(), params
		);
	}

	@Override
	protected void collectRelationalValueBindings(List<RelationalValueBinding> valueBindings) {
		valueBindings.addAll( relationalValueBindings );
	}
}
