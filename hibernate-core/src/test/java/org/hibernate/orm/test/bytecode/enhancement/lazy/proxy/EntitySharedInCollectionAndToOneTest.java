/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@JiraKey( "HHH-11147" )
@DomainModel(
		annotatedClasses = {
				EntitySharedInCollectionAndToOneTest.CodeTableItem.class,
				EntitySharedInCollectionAndToOneTest.CodeTable.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.FORMAT_SQL, value = "false" ),
				@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false" ),
				@Setting( name = AvailableSettings.USE_QUERY_CACHE, value = "false" ),
		}
)
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions( lazyLoading = true )
public class EntitySharedInCollectionAndToOneTest {

	@Test
	public void testIt(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					int passes = 0;
					for ( CodeTable codeTable : session.createQuery( "from CodeTable ct where ct.id = 2", CodeTable.class ).list() ) {
						assert 0 == passes;
						passes++;
						Hibernate.initialize( codeTable.getCodeTableItems() );
					}

					assertThat( session.getPersistenceContext().getNumberOfManagedEntities(), is( 2 ) );
				}
		);
	}

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final CodeTable codeTable1 = new CodeTable( 1, 1 );
					final CodeTableItem item1 = new CodeTableItem( 1, 1, "first" );
					final CodeTableItem item2 = new CodeTableItem( 2, 1, "second" );
					final CodeTableItem item3 = new CodeTableItem( 3, 1, "third" );

					session.persist( codeTable1 );
					session.persist( item1 );
					session.persist( item2 );
					session.persist( item3 );

					codeTable1.getCodeTableItems().add( item1 );
					item1.setCodeTable( codeTable1 );

					codeTable1.getCodeTableItems().add( item2 );
					item2.setCodeTable( codeTable1 );

					codeTable1.getCodeTableItems().add( item3 );
					item3.setCodeTable( codeTable1 );

					codeTable1.setDefaultItem( item1 );
					item1.setDefaultItemInverse( codeTable1 );

					final CodeTable codeTable2 = new CodeTable( 2, 1 );
					final CodeTableItem item4 = new CodeTableItem( 4, 1, "fourth" );

					session.persist( codeTable2 );
					session.persist( item4 );

					codeTable2.getCodeTableItems().add( item4 );
					item4.setCodeTable( codeTable2 );

					codeTable2.setDefaultItem( item4 );
					item4.setDefaultItemInverse( codeTable2 );
				}
		);
	}

//	@After
//	public void deleteTestData() {
//		inTransaction(
//				session -> {
//					for ( CodeTable codeTable : session.createQuery( "from CodeTable", CodeTable.class ).list() ) {
//						session.remove( codeTable );
//					}
//				}
//		);
//	}

	@MappedSuperclass
	public static class BaseEntity {
		@Id
		private Integer oid;
		private int version;

		public BaseEntity() {
		}

		public BaseEntity(Integer oid, int version) {
			this.oid = oid;
			this.version = version;
		}

		public Integer getOid() {
			return oid;
		}

		public void setOid(Integer oid) {
			this.oid = oid;
		}

		public int getVersion() {
			return version;
		}

		public void setVersion(int version) {
			this.version = version;
		}
	}

	@Entity( name = "CodeTable" )
	@Table( name = "code_table" )
	public static class CodeTable extends BaseEntity {
		@OneToOne( fetch = FetchType.LAZY )
		@LazyGroup( "defaultCodeTableItem" )
		@JoinColumn( name = "default_code_id" )
		private CodeTableItem defaultItem;

		@OneToMany( mappedBy = "codeTable" )
		private Set<CodeTableItem> codeTableItems = new HashSet<>();

		public CodeTable() {
		}

		public CodeTable(Integer oid, int version) {
			super( oid, version );
		}

		public CodeTableItem getDefaultItem() {
			return defaultItem;
		}

		public void setDefaultItem(CodeTableItem defaultItem) {
			this.defaultItem = defaultItem;
		}

		public Set<CodeTableItem> getCodeTableItems() {
			return codeTableItems;
		}

		public void setCodeTableItems(Set<CodeTableItem> codeTableItems) {
			this.codeTableItems = codeTableItems;
		}
	}

	@Entity( name = "CodeTableItem" )
	@Table( name = "code_table_item" )
	public static class CodeTableItem extends BaseEntity {
		private String name;

		@ManyToOne( fetch = FetchType.LAZY )
		@LazyGroup( "codeTable" )
		@JoinColumn( name = "code_table_oid" )
		private CodeTable codeTable;

		@OneToOne( mappedBy = "defaultItem", fetch=FetchType.LAZY )
		@LazyGroup( "defaultItemInverse" )
		protected CodeTable defaultItemInverse;


		public CodeTableItem() {
		}

		public CodeTableItem(Integer oid, int version, String name) {
			super( oid, version );
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public CodeTable getCodeTable() {
			return codeTable;
		}

		public void setCodeTable(CodeTable codeTable) {
			this.codeTable = codeTable;
		}

		public CodeTable getDefaultItemInverse() {
			return defaultItemInverse;
		}

		public void setDefaultItemInverse(CodeTable defaultItemInverse) {
			this.defaultItemInverse = defaultItemInverse;
		}
	}
}
