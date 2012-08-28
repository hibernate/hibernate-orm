/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.binding;

import java.sql.Types;

import org.junit.Test;

import org.hibernate.EntityMode;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.metamodel.domain.Entity;
import org.hibernate.metamodel.domain.SingularAttribute;
import org.hibernate.metamodel.relational.Column;
import org.hibernate.metamodel.relational.Datatype;
import org.hibernate.metamodel.relational.Schema;
import org.hibernate.metamodel.relational.Size;
import org.hibernate.metamodel.relational.Table;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertSame;

/**
 * Basic binding "smoke" tests
 *
 * @author Steve Ebersole
 */
public class SimpleValueBindingTests extends BaseUnitTestCase {
	public static final Datatype BIGINT = new Datatype( Types.BIGINT, "BIGINT", Long.class );
	public static final Datatype VARCHAR = new Datatype( Types.VARCHAR, "VARCHAR", String.class );


	@Test
	public void testBasicMiddleOutBuilding() {
		Table table = new Table( new Schema( null, null ), "the_table" );
		Entity entity = new Entity( "TheEntity", "NoSuchClass", makeJavaType( "NoSuchClass" ), null );
		EntityBinding entityBinding = new EntityBinding( InheritanceType.NO_INHERITANCE, EntityMode.POJO );
		entityBinding.setEntity( entity );
		entityBinding.setPrimaryTable( table );

		SingularAttribute idAttribute = entity.createSingularAttribute( "id" );
		BasicAttributeBinding attributeBinding = entityBinding.makeBasicAttributeBinding( idAttribute );
		attributeBinding.getHibernateTypeDescriptor().setExplicitTypeName( "long" );
		assertSame( idAttribute, attributeBinding.getAttribute() );

		entityBinding.getHierarchyDetails().getEntityIdentifier().setValueBinding( attributeBinding );

		Column idColumn = table.locateOrCreateColumn( "id" );
		idColumn.setDatatype( BIGINT );
		idColumn.setSize( Size.precision( 18, 0 ) );
		table.getPrimaryKey().addColumn( idColumn );
		table.getPrimaryKey().setName( "my_table_pk" );
		//attributeBinding.setValue( idColumn );
	}

	ValueHolder<Class<?>> makeJavaType(final String name) {
		return new ValueHolder<Class<?>>(
				new ValueHolder.DeferredInitializer<Class<?>>() {
					@Override
					public Class<?> initialize() {
						try {
							return Class.forName( name );
						}
						catch ( Exception e ) {
							throw new ClassLoadingException( "Could not load class : " + name, e );
						}
					}
				}
		);
	}
}
