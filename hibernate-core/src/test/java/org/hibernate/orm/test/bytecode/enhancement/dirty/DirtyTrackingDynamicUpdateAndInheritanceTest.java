/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.dirty;

import java.util.List;

import org.hibernate.annotations.DynamicUpdate;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				DirtyTrackingDynamicUpdateAndInheritanceTest.SuperEntity.class,
				DirtyTrackingDynamicUpdateAndInheritanceTest.ChildEntity.class,
				DirtyTrackingDynamicUpdateAndInheritanceTest.AbstractVersion.class,
				DirtyTrackingDynamicUpdateAndInheritanceTest.FileVersion.class
		}
)
@SessionFactory(
		// We want to test with this setting set to false explicitly,
		// because another test already takes care of the default.
		applyCollectionsInDefaultFetchGroup = false
)
@BytecodeEnhanced
@EnhancementOptions(inlineDirtyChecking = true)
public class DirtyTrackingDynamicUpdateAndInheritanceTest {

	public static final int ID = 1;

	@Test
	@JiraKey("HHH-16688")
	public void testDynamicUpdateWithInheritance(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					ChildEntity entity = new ChildEntity( ID );
					entity.setaSuper( "aSuper before" );
					entity.setbSuper( "bSuper before" );
					entity.setaChild( "aChild before" );
					entity.setbChild( "bChild before" );
					session.persist( entity );
				}
		);

		String aSuperNewValue = "aSuper after";
		String bSuperNewValue = "bSuper after";
		String aChildNewValue = "aChild after";
		String bChildNewValue = "bChild after";

		scope.inTransaction(
				session -> {
					ChildEntity entity = session.find( ChildEntity.class, ID );
					entity.setaSuper( aSuperNewValue );
					entity.setbSuper( bSuperNewValue );
					entity.setaChild( aChildNewValue );
					entity.setbChild( bChildNewValue );
					session.merge( entity );
				}
		);

		scope.inTransaction(
				session -> {
					ChildEntity entity = session.find( ChildEntity.class, ID );
					assertThat( entity.getaSuper() ).isEqualTo( aSuperNewValue );
					assertThat( entity.getbSuper() ).isEqualTo( bSuperNewValue );
					assertThat( entity.getaChild() ).isEqualTo( aChildNewValue );
					assertThat( entity.getbChild() ).isEqualTo( bChildNewValue );
				}
		);
	}

	@Test
	@JiraKey("HHH-16379")
	public void testWithDynamicUpdate(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					FileVersion version = new FileVersion();
					version.setId( "1" );
					version.setFileSize( 100L );
					version.setCheckSumMD5( "ABCDEF" );
					version.setVariantName( "A" );
					session.persist( version );

					session.flush();

					// update version
					session.evict( version );
					List<FileVersion> result = session.createQuery( "from FileVersion", FileVersion.class )
							.getResultList();
					assertThat( result.size() ).isEqualTo( 1 );

					version = result.get( 0 );
					version.setCheckSumMD5( "XXXXXXXX" );
					version.setVariantName( "B" );
					version.setFileSize( 200L );
					session.persist( version );
					session.flush();

					session.evict( version );
					result = session.createQuery( "from FileVersion", FileVersion.class )
							.getResultList();
					assertThat( result.size() ).isEqualTo( 1 );

					version = result.get( 0 );
					assertThat( version.getCheckSumMD5() ).isEqualTo( "XXXXXXXX" );
					assertThat( version.getVariantName() ).isEqualTo( "B" );
					assertThat( version.getFileSize() ).isEqualTo( 200L );
				}
		);
	}


	@Entity(name = "SuperEntity")
	public abstract static class SuperEntity {
		@Id
		private Integer id;
		private String bSuper;
		private String aSuper;

		public SuperEntity() {
		}

		public SuperEntity(Integer id) {
			this.id = id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getaSuper() {
			return aSuper;
		}

		public void setaSuper(String aSuper) {
			this.aSuper = aSuper;
		}

		public String getbSuper() {
			return bSuper;
		}

		public void setbSuper(String bSuper) {
			this.bSuper = bSuper;
		}
	}

	@Entity(name = "ChildEntity")
	@DynamicUpdate
	public static class ChildEntity extends SuperEntity {
		private String aChild;
		private String bChild;

		public ChildEntity() {
		}

		public ChildEntity(Integer id) {
			super( id );
		}

		public String getaChild() {
			return aChild;
		}

		public void setaChild(String aChild) {
			this.aChild = aChild;
		}

		public String getbChild() {
			return bChild;
		}

		public void setbChild(String bChild) {
			this.bChild = bChild;
		}
	}

	@Entity(name = "AbstractVersion")
	@Table(name = "Versionen")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "REPRAESENTATION", discriminatorType = DiscriminatorType.STRING)
	@DynamicUpdate()
	public static abstract class AbstractVersion {
		@Id
		@Column(name = "VERS_NUMMER")
		private String id;

		@Column(name = "CHECKSUM")
		protected String checkSumMD5;

		@Column(name = "VARIANTE", nullable = false)
		private String variantName;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getCheckSumMD5() {
			return checkSumMD5;
		}

		public void setCheckSumMD5(String checkSumMD5) {
			this.checkSumMD5 = checkSumMD5;
		}

		public String getVariantName() {
			return variantName;
		}

		public void setVariantName(String variantName) {
			this.variantName = variantName;
		}
	}

	@Entity(name = "FileVersion")
	@DiscriminatorValue("F")
	@DynamicUpdate()
	public static class FileVersion extends AbstractVersion {
		@Column(name = "FILESIZE")
		private Long fileSize;

		public Long getFileSize() {
			return fileSize;
		}

		public void setFileSize(Long fileSize) {
			this.fileSize = fileSize;
		}
	}
}
