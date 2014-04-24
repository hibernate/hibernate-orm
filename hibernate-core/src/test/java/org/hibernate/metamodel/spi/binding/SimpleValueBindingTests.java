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
package org.hibernate.metamodel.spi.binding;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.internal.ClassLoaderAccessImpl;
import org.hibernate.metamodel.reflite.internal.JavaTypeDescriptorRepositoryImpl;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptorRepository;
import org.hibernate.metamodel.source.internal.annotations.JandexAccessImpl;
import org.hibernate.metamodel.spi.NaturalIdMutability;
import org.hibernate.metamodel.spi.domain.Entity;
import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.metamodel.spi.relational.JdbcDataType;
import org.hibernate.metamodel.spi.relational.Schema;
import org.hibernate.metamodel.spi.relational.Size;
import org.hibernate.metamodel.spi.relational.Table;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertSame;

/**
 * Basic binding "smoke" tests
 *
 * @author Steve Ebersole
 */
public class SimpleValueBindingTests extends BaseUnitTestCase {
	public static final JdbcDataType BIGINT = new JdbcDataType( Types.BIGINT, "BIGINT", Long.class );
	public static final JdbcDataType VARCHAR = new JdbcDataType( Types.VARCHAR, "VARCHAR", String.class );

	private static final ClassLoaderService classLoaderService = new ClassLoaderServiceImpl();


	@Test
	public void testBasicMiddleOutBuilding() {
		final Identifier tableName = Identifier.toIdentifier( "the_table" );
		Table table = new Table( new Schema( null, null ), tableName, tableName );
		Column idColumn = table.locateOrCreateColumn( "id" );
		idColumn.setJdbcDataType( BIGINT );
		idColumn.setSize( Size.precision( 18, 0 ) );
		table.getPrimaryKey().addColumn( idColumn );
		table.getPrimaryKey().setName( Identifier.toIdentifier( "my_table_pk" ) );

		Entity entity = new Entity( makeJavaType( "NoSuchClass" ), null );
		HierarchyDetails hierarchyDetails = new HierarchyDetails.Builder().createHierarchyDetails();
		EntityBinding entityBinding = hierarchyDetails.getRootEntityBinding();
		entityBinding.setEntity( entity );
		entityBinding.setPrimaryTable( table );

		List<RelationalValueBinding> valueBindings = new ArrayList<RelationalValueBinding>();
		valueBindings.add(
				new RelationalValueBinding(
						table,
						idColumn,
						true,
						true
				)
		);
		SingularAttribute idAttribute = entity.createSingularAttribute( "id" );
		BasicAttributeBinding attributeBinding = entityBinding.makeBasicAttributeBinding(
				idAttribute,
				valueBindings,
				"property",
				true,
				false,
				NaturalIdMutability.NOT_NATURAL_ID,
				null,
				entityBinding.getRoleBase().append( "id" ),
				entityBinding.getPathBase().append( "id" ),
				PropertyGeneration.NEVER
		);
		attributeBinding.getHibernateTypeDescriptor().setExplicitTypeName( "long" );
		assertSame( idAttribute, attributeBinding.getAttribute() );

		entityBinding.getHierarchyDetails().getEntityIdentifier().prepareAsSimpleIdentifier(
				attributeBinding,
				new IdentifierGeneratorDefinition( "assigned", "assigned", Collections.<String,String>emptyMap() ),
				"null"
		);
	}

	private final ClassLoaderAccessImpl classLoaderAccess = new ClassLoaderAccessImpl( null, classLoaderService );
	private final JavaTypeDescriptorRepository javaTypeDescriptorRepository = new JavaTypeDescriptorRepositoryImpl(
			new JandexAccessImpl( null, classLoaderAccess ),
			classLoaderAccess
	);

	JavaTypeDescriptor makeJavaType(final String name) {
		return javaTypeDescriptorRepository.getType( javaTypeDescriptorRepository.buildName( name ) );
	}
}
