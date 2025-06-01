/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.constraint;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.ToOne;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.internal.SimpleForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Simplified form of mappings from {@link ForeignKeyNoConstraintTest}
 * specifically for checking definition of foreign-keys related
 * to non-root tables within a joined-hierarchy.
 *
 * It tries to get to the bottom of the failures in
 * {@link ForeignKeyConstraintTest#testGet}
 */
@JiraKey( value = "HHH-11180" )
@DomainModel(
		annotatedClasses = {
				NonRootTablePolymorphicTests.Root.class,
				NonRootTablePolymorphicTests.Sub.class,
				NonRootTablePolymorphicTests.Leaf.class,
				NonRootTablePolymorphicTests.SubParent.class,
				NonRootTablePolymorphicTests.SubChild.class,
				NonRootTablePolymorphicTests.SubGroup.class,
		}
)
@SessionFactory
public class NonRootTablePolymorphicTests {
	@Test
	public void verifyBootModel(DomainModelScope scope) {
		// check the foreign-keys within the joined-hierarchy
		scope.withHierarchy(
				Root.class,
				(root) -> {
					final org.hibernate.mapping.Table rootRootTable = root.getRootTable();
					final org.hibernate.mapping.Table rootTable = root.getTable();
					assertThat( rootRootTable, sameInstance( rootTable ) );
					assertThat( rootRootTable.getName(), is( "root" ) );

					for ( Subclass value : root.getSubclasses() ) {
						final JoinedSubclass subclass = (JoinedSubclass) value;
						final org.hibernate.mapping.Table subclassTable = subclass.getTable();

						if ( subclass.getJpaEntityName().equals( "Sub" ) ) {
							assertThat( subclass.getRootTable(), sameInstance( rootTable ) );
							assertThat( subclassTable, not( sameInstance( rootTable ) ) );
							assertThat( subclassTable.getName(), is( "sub" ) );

							final PrimaryKey primaryKey = subclassTable.getPrimaryKey();
							assertThat( primaryKey.getColumnSpan(), is( 1 ) );
							assertThat( primaryKey.getColumn( 0 ).getName(), is( "sub_id" ) );

							// the sub table should have 2 foreign-keys defined:
							//		1) for the sub->root inheritance fk
							//		2) for the sub->child fk

							assertThat( subclassTable.getForeignKeyCollection().size(), is( 2 ) );
							subclassTable.getForeignKeyCollection().iterator().forEachRemaining(
									(foreignKey) -> {
										assertThat( foreignKey.getTable(), sameInstance( subclassTable ) );

										// see which we have...
										if ( foreignKey.getReferencedTable().getName().equals( "root" ) ) {
											assertThat( foreignKey.getReferencedTable(), sameInstance( rootTable ) );

											assertThat( foreignKey.getColumns().get( 0 ).getName(), is( "sub_id" ) );

											// this is how the boot model represents an fk pointing to the pk..
											assertThat(
													foreignKey.getReferencedColumns(),
													anyOf(
															nullValue(),
															CollectionMatchers.isEmpty()
													)
											);
										}
										else if ( foreignKey.getReferencedTable().getName().equals( "sub_child" ) ) {
											assertThat( foreignKey.getColumns().get( 0 ).getName(), is( "child_fk" ) );

											// this is how the boot model represents an fk pointing to the pk..
											assertThat(
													foreignKey.getReferencedColumns(),
													anyOf(
															nullValue(),
															CollectionMatchers.isEmpty()
													)
											);
										}
										else {
											fail( "Unexpected fk reference from `sub` to `" + foreignKey.getReferencedTable().getName() + "`" );
										}
									}
							);
						}
						else if ( subclass.getJpaEntityName().equals( "Leaf" ) ) {
							assertThat( subclass.getRootTable(), sameInstance( rootTable ) );
							assertThat( subclassTable, not( sameInstance( rootTable ) ) );
							assertThat( subclassTable.getName(), is( "leaf" ) );

							final PrimaryKey primaryKey = subclassTable.getPrimaryKey();
							assertThat( primaryKey.getColumnSpan(), is( 1 ) );
							assertThat( primaryKey.getColumn( 0 ).getName(), is( "leaf_id" ) );
						}
					}
				}
		);

		// check the association *to* the joined-hierarchy (aka, SubParent#sub)
		//		- we already checked the association *from* earlier
		scope.withHierarchy(
				SubParent.class,
				(subParent) -> {
					final Property subProperty = subParent.getProperty( "sub" );
					final ToOne toOne = (ToOne) subProperty.getValue();

					assertThat( toOne.getTable().getName(), is( "sub_parent" ) );

					assertThat( toOne.getColumnSpan(), is( 1 ) );
					final Selectable selectable = toOne.getSelectables().get( 0 );
					assertThat( selectable.getText(), is( "parent_sub_fk" ) );

					assertThat( subParent.getTable().getForeignKeyCollection().size(), is( 1 ) );
					final ForeignKey foreignKey = subParent.getTable().getForeignKeyCollection().iterator().next();

					assertThat( foreignKey.getReferencedTable().getName(), is( "sub" ) );
					assertThat( foreignKey.getTable(), sameInstance( toOne.getTable() ) );
				}
		);
	}


	@Test
	public void verifyRuntimeModel(SessionFactoryScope scope) {
		final EntityMappingType rootEntity = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getEntityMappingType( Root.class );
		final EntityMappingType subEntity = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getEntityMappingType( Sub.class );
		final EntityMappingType leafEntity = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getEntityMappingType( Leaf.class );
		final EntityMappingType childEntity = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getEntityMappingType( SubChild.class );
		final EntityMappingType parentEntity = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getEntityMappingType( SubParent.class );


		// check Sub#child fk

		final ToOneAttributeMapping childAttribute = (ToOneAttributeMapping) subEntity.findAttributeMapping( "child" );
		final SimpleForeignKeyDescriptor childFk = (SimpleForeignKeyDescriptor) childAttribute.getForeignKeyDescriptor();

		assertThat( childFk.getKeyTable(), is( "sub" ) );
		assertThat( childFk.getTargetTable(), is( "sub_child" ) );

		assertThat( childFk.getJdbcTypeCount(), is( 1 ) );
		assertThat( childFk.getKeyPart().getSelectionExpression(), is( "child_fk" ) );
		assertThat( childFk.getTargetPart().getSelectionExpression(), is( "child_id" ) );


		// check Parent#sub fk

		final ToOneAttributeMapping subAttribute = (ToOneAttributeMapping) parentEntity.findAttributeMapping( "sub" );
		final SimpleForeignKeyDescriptor subFk = (SimpleForeignKeyDescriptor) subAttribute.getForeignKeyDescriptor();

		assertThat( subFk.getKeyTable(), is( "sub_parent" ) );
		assertThat( subFk.getTargetTable(), is( "sub" ) );

		assertThat( subFk.getJdbcTypeCount(), is( 1 ) );
		assertThat( subFk.getKeyPart().getSelectionExpression(), is( "parent_sub_fk" ) );
		assertThat( subFk.getTargetPart().getSelectionExpression(), is( "sub_id" ) );

		scope.inTransaction(
				(session) -> {
					session.createQuery( "from SubParent p join fetch p.sub" ).list();
					session.createQuery( "from SubGroup p join fetch p.manyToManySubs" ).list();
					session.createQuery( "from SubGroup p join fetch p.oneToManySubs" ).list();
				}
		);

		// for sure the inheritance keys are messed up in the mapping model
	}

	@Entity( name = "Root" )
	@Table( name = "root" )
	@Inheritance( strategy = InheritanceType.JOINED )
	public static class Root {
		@Id
		@jakarta.persistence.Column( name = "root_id" )
		private Integer id;
		private String name;
	}

	@Entity( name = "Sub" )
	@Table( name = "sub" )
	@PrimaryKeyJoinColumn( name = "sub_id", referencedColumnName = "root_id" )
	public static class Sub extends Root {
		private String subText;

		@ManyToOne
		@JoinColumn( name = "child_fk", referencedColumnName = "child_id" )
		public SubChild child;
	}

	@Entity( name = "Leaf" )
	@Table( name = "leaf" )
	@PrimaryKeyJoinColumn( name = "leaf_id", referencedColumnName = "root_id" )
	public static class Leaf extends Root {
		private String teaLeaves;
	}


	@Entity( name = "SubParent" )
	@Table( name = "sub_parent" )
	public static class SubParent {
		@Id
		@jakarta.persistence.Column( name = "parent_id" )
		private Integer id;
		private String name;
		@ManyToOne
		@JoinColumn( name = "parent_sub_fk", referencedColumnName = "sub_id" )
		public Sub sub;
	}


	@Entity( name = "SubChild" )
	@Table( name = "sub_child" )
	public static class SubChild {
		@Id
		@jakarta.persistence.Column( name = "child_id" )
		private Integer id;
		private String name;
	}

	@Entity( name = "SubGroup" )
	@Table( name = "sub_group" )
	public static class SubGroup {
		@Id
		@jakarta.persistence.Column( name = "group_id" )
		private Integer id;
		private String name;

		@ManyToMany
		@JoinTable(
				name = "m2m_group",
				joinColumns = @JoinColumn( name = "m2m_group_fk", referencedColumnName = "group_id" ),
				inverseJoinColumns = @JoinColumn( name = "m2m_sub_fk", referencedColumnName = "sub_id" )
		)
		private List<Sub> manyToManySubs;

		@OneToMany
		@PrimaryKeyJoinColumn( name = "m_group_fk", referencedColumnName = "group_id" )
		private List<Sub> oneToManySubs;
	}

}
