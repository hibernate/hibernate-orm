/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-11147" )
@RunWith( BytecodeEnhancerRunner.class )
@EnhancementOptions( lazyLoading = true )
public class EntitySharedInCollectionAndToOneTest extends BaseNonConfigCoreFunctionalTestCase {
	@Test
	public void testIt() {
		inTransaction(
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

	@Before
	public void createTestData() {
		inTransaction(
				session -> {
					final CodeTable codeTable1 = new CodeTable( 1, 1 );
					final CodeTableItem item1 = new CodeTableItem( 1, 1, "first" );
					final CodeTableItem item2 = new CodeTableItem( 2, 1, "second" );
					final CodeTableItem item3 = new CodeTableItem( 3, 1, "third" );

					session.save( codeTable1 );
					session.save( item1 );
					session.save( item2 );
					session.save( item3 );

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

					session.save( codeTable2 );
					session.save( item4 );

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
//						session.delete( codeTable );
//					}
//				}
//		);
//	}

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.ALLOW_ENHANCEMENT_AS_PROXY, "true" );
		ssrb.applySetting( AvailableSettings.FORMAT_SQL, "false" );
	}

	@Override
	protected void configureSessionFactoryBuilder(SessionFactoryBuilder sfb) {
		super.configureSessionFactoryBuilder( sfb );
		sfb.applyStatisticsSupport( true );
		sfb.applySecondLevelCacheSupport( false );
		sfb.applyQueryCacheSupport( false );
	}

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		super.applyMetadataSources( sources );
		sources.addAnnotatedClass( CodeTableItem.class );
		sources.addAnnotatedClass( CodeTable.class );
	}

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
		@LazyToOne( LazyToOneOption.NO_PROXY )
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
