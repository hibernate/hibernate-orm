/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.cascade.multilevel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class MultiLevelCascadeCollectionIdClassTest extends BaseEntityManagerFunctionalTestCase {

	private static final Logger log = Logger.getLogger( MultiLevelCascadeCollectionIdClassTest.class );

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				MainEntity.class,
				SubEntity.class,
				AnotherSubSubEntity.class,
				SubSubEntity.class
		};
	}

	protected void afterEntityManagerFactoryBuilt() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.createNativeQuery( "INSERT INTO MAIN_TABLE(ID_NUM) VALUES (99427)" ).executeUpdate();
			entityManager.createNativeQuery( "INSERT INTO SUB_TABLE(ID_NUM, SUB_ID, FAMILY_IDENTIFIER, IND_NUM) VALUES (99427, 1, 'A', '123A')" ).executeUpdate();
			entityManager.createNativeQuery( "INSERT INTO SUB_TABLE(ID_NUM, SUB_ID, FAMILY_IDENTIFIER, IND_NUM) VALUES (99427, 2, 'S', '321A')" ).executeUpdate();
			entityManager.createNativeQuery( "INSERT INTO SUB_SUB_TABLE(ID_NUM, CODE, IND_NUM) VALUES (99427, 'CODE1', '123A')" ).executeUpdate();
		} );
	}

	@Test
	@FailureExpected( jiraKey = "HHH-12291" )
	public void testHibernateDeleteEntityWithoutInitializingCollections() throws Exception {
		doInJPA( this::entityManagerFactory, entityManager -> {
			MainEntity mainEntity = entityManager.find(MainEntity.class, 99427L);

			assertNotNull(mainEntity);
			assertFalse(mainEntity.getSubEntities().isEmpty());

			Optional<SubEntity> subEntityToRemove =  mainEntity.getSubEntities().stream()
					.filter(subEntity -> "123A".equals(subEntity.getIndNum())).findFirst();
			subEntityToRemove.ifPresent( mainEntity::removeSubEntity );
		} );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12294" )
	public void testHibernateDeleteEntityInitializeCollections() throws Exception {
		doInJPA( this::entityManagerFactory, entityManager -> {
			MainEntity mainEntity = entityManager.find(MainEntity.class, 99427L);

			assertNotNull(mainEntity);
			assertFalse(mainEntity.getSubEntities().isEmpty());

			Optional<SubEntity> subEntityToRemove =  mainEntity.getSubEntities().stream()
					.filter(subEntity -> "123A".equals(subEntity.getIndNum())).findFirst();
			if ( subEntityToRemove.isPresent() ) {
				SubEntity subEntity = subEntityToRemove.get();
				assertEquals( 1, subEntity.getSubSubEntities().size() );
				assertEquals( 0, subEntity.getAnotherSubSubEntities().size() );
				mainEntity.removeSubEntity( subEntity );
			}
		} );
	}


	@Entity
	@Table(name = "MAIN_TABLE")
	public static class MainEntity implements Serializable {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "idNum")
		@SequenceGenerator(name = "idNum", sequenceName = "id_num", allocationSize = 1)
		@Column(name="ID_NUM")
		private Long idNum;

		@OneToMany(mappedBy = "mainEntity", cascade = CascadeType.ALL, orphanRemoval = true)
		private List<SubEntity> subEntities = new ArrayList<>();

		public void addSubEntity(SubEntity subEntity) {
			subEntity.setMainEntity(this);
			subEntities.add(subEntity);
		}

		public void removeSubEntity(SubEntity subEntity) {
			subEntity.setMainEntity(null);
			subEntities.remove(subEntity);
		}

		public Long getIdNum() {
			return idNum;
		}

		public void setIdNum(Long idNum) {
			this.idNum = idNum;
		}

		public List<SubEntity> getSubEntities() {
			return subEntities;
		}

		public void setSubEntities(List<SubEntity> subEntities) {
			this.subEntities = subEntities;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			if (!super.equals(o)) return false;
			MainEntity that = (MainEntity) o;
			return Objects.equals(idNum, that.idNum);
		}

		@Override
		public int hashCode() {

			return Objects.hash(super.hashCode(), idNum);
		}
	}

	@Entity
	@Table(name = "SUB_TABLE")
	public static class SubEntity implements Serializable {

		@Id
		@Column(name = "SUB_ID")
		private Long subIdNum;

		@Column(name = "IND_NUM")
		private String indNum;

		@Column(name = "FAMILY_IDENTIFIER")
		private String familyIdentifier;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "ID_NUM")
		private MainEntity mainEntity;

		@OneToMany(mappedBy = "subEntity", cascade = CascadeType.ALL, orphanRemoval = true)
		private List<SubSubEntity> subSubEntities = new ArrayList<>();

		@OneToMany(mappedBy = "subEntity", cascade = CascadeType.ALL, orphanRemoval = true)
		private List<AnotherSubSubEntity> anotherSubSubEntities = new ArrayList<>();

		public Long getSubIdNum() {
			return subIdNum;
		}

		public void setSubIdNum(Long subIdNum) {
			this.subIdNum = subIdNum;
		}

		public String getIndNum() {
			return indNum;
		}

		public void setIndNum(String indNum) {
			this.indNum = indNum;
		}

		public String getFamilyIdentifier() {
			return familyIdentifier;
		}

		public void setFamilyIdentifier(String familyIdentifier) {
			this.familyIdentifier = familyIdentifier;
		}

		public MainEntity getMainEntity() {
			return mainEntity;
		}

		public void setMainEntity(MainEntity mainEntity) {
			this.mainEntity = mainEntity;
		}

		public List<SubSubEntity> getSubSubEntities() {
			return subSubEntities;
		}

		public void setSubSubEntities(List<SubSubEntity> subSubEntities) {
			this.subSubEntities = subSubEntities;
		}

		public List<AnotherSubSubEntity> getAnotherSubSubEntities() {
			return anotherSubSubEntities;
		}

		public void setAnotherSubSubEntities(List<AnotherSubSubEntity> anotherSubSubEntities) {
			this.anotherSubSubEntities = anotherSubSubEntities;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			if (!super.equals(o)) return false;
			SubEntity subEntity = (SubEntity) o;
			return Objects.equals(subIdNum, subEntity.subIdNum);
		}

		@Override
		public int hashCode() {

			return Objects.hash(super.hashCode(), subIdNum);
		}
	}

	@Entity
	@Table(name = "ANOTHER_SUB_SUB_TABLE")
	@IdClass(AnotherSubSubEntity.AnotherSubSubEntityId.class)
	public static class AnotherSubSubEntity implements Serializable {

		@Id
		@Column(name = "ID_NUM", insertable = false, updatable = false)
		private Long idNum;

		@Id
		@Column(name = "PERSON", insertable = false, updatable = false)
		private String person;

		@Id
		@Column(name = "SOURCE_CODE")
		private String sourceCode;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumns({
				@JoinColumn(name = "ID_NUM", referencedColumnName = "ID_NUM", insertable = false, updatable = false),
				@JoinColumn(name = "PERSON", referencedColumnName = "FAMILY_IDENTIFIER", insertable = false, updatable = false)
		})
		private SubEntity subEntity;

		public Long getIdNum() {
			return idNum;
		}

		public void setIdNum(Long idNum) {
			this.idNum = idNum;
		}

		public String getPerson() {
			return person;
		}

		public void setPerson(String person) {
			this.person = person;
		}

		public String getSourceCode() {
			return sourceCode;
		}

		public void setSourceCode(String sourceCode) {
			this.sourceCode = sourceCode;
		}

		public SubEntity getSubEntity() {
			return subEntity;
		}

		public void setSubEntity(SubEntity subEntity) {
			idNum = Optional.ofNullable(subEntity).map( SubEntity::getMainEntity).map( MainEntity::getIdNum).orElse( null);
			person = Optional.ofNullable(subEntity).map( SubEntity::getFamilyIdentifier).orElse( null);
			this.subEntity = subEntity;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			AnotherSubSubEntity that = (AnotherSubSubEntity) o;
			return Objects.equals(idNum, that.idNum) &&
					Objects.equals(person, that.person) &&
					Objects.equals(sourceCode, that.sourceCode);
		}

		@Override
		public int hashCode() {
			return Objects.hash(idNum, person, sourceCode);
		}

		@Override
		public String toString() {
			return "AnotherSubSubEntity{" +
					"idNum=" + idNum +
					", person='" + person + '\'' +
					", sourceCode='" + sourceCode + '\'' +
					'}';
		}

		public static class AnotherSubSubEntityId implements Serializable {

			private Long idNum;

			private String person;

			private String sourceCode;

			@Override
			public boolean equals(Object o) {
				if (this == o) return true;
				if (o == null || getClass() != o.getClass()) return false;
				AnotherSubSubEntityId that = (AnotherSubSubEntityId) o;
				return Objects.equals(idNum, that.idNum) &&
						Objects.equals(person, that.person) &&
						Objects.equals(sourceCode, that.sourceCode);
			}

			@Override
			public int hashCode() {
				return Objects.hash(idNum, person, sourceCode);
			}

			public Long getIdNum() {
				return idNum;
			}

			public void setIdNum(Long idNum) {
				this.idNum = idNum;
			}

			public String getPerson() {
				return person;
			}

			public void setPerson(String person) {
				this.person = person;
			}

			public String getSourceCode() {
				return sourceCode;
			}

			public void setSourceCode(String sourceCode) {
				this.sourceCode = sourceCode;
			}
		}

	}

	@Entity
	@Table(name = "SUB_SUB_TABLE")
	@IdClass(SubSubEntity.SubSubEntityId.class)
	public static class SubSubEntity implements Serializable {

		@Id
		@Column(name = "ID_NUM", insertable = false, updatable = false)
		private Long idNum;

		@Id
		@Column(name = "CODE")
		private String code;

		@Id
		@Column(name = "IND_NUM", insertable = false, updatable = false)
		private String indNum;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumns({
				@JoinColumn(name = "ID_NUM", referencedColumnName = "ID_NUM"),
				@JoinColumn(name = "IND_NUM", referencedColumnName = "IND_NUM")
		})
		private SubEntity subEntity;

		public Long getIdNum() {
			return idNum;
		}

		public void setIdNum(Long idNum) {
			this.idNum = idNum;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}

		public String getIndNum() {
			return indNum;
		}

		public void setIndNum(String indNum) {
			this.indNum = indNum;
		}

		public SubEntity getSubEntity() {
			return subEntity;
		}

		public void setSubEntity(SubEntity subEntity) {
			idNum = Optional.ofNullable(subEntity).map( SubEntity::getMainEntity).map( MainEntity::getIdNum).orElse( null);
			code = Optional.ofNullable(subEntity).map( SubEntity::getIndNum).orElse( null);
			this.subEntity = subEntity;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			if (!super.equals(o)) return false;
			SubSubEntity that = (SubSubEntity) o;
			return Objects.equals(idNum, that.idNum) &&
					Objects.equals(code, that.code) &&
					Objects.equals(indNum, that.indNum);
		}

		@Override
		public int hashCode() {
			return Objects.hash(super.hashCode(), idNum, code, indNum);
		}

		@Override
		public String toString() {
			return "SubSubEntity{" +
					"idNum=" + idNum +
					", code='" + code + '\'' +
					", indNum='" + indNum + '\'' +
					'}';
		}

		public static class SubSubEntityId implements Serializable {

			private Long idNum;

			private String code;

			private String indNum;

			public Long getIdNum() {
				return idNum;
			}

			public void setIdNum(Long idNum) {
				this.idNum = idNum;
			}

			public String getCode() {
				return code;
			}

			public void setCode(String code) {
				this.code = code;
			}

			public String getIndNum() {
				return indNum;
			}

			public void setIndNum(String indNum) {
				this.indNum = indNum;
			}

			@Override
			public boolean equals(Object o) {
				if (this == o) return true;
				if (o == null || getClass() != o.getClass()) return false;
				SubSubEntityId that = (SubSubEntityId) o;
				return Objects.equals(idNum, that.idNum) &&
						Objects.equals(code, that.code) &&
						Objects.equals(indNum, that.indNum);
			}

			@Override
			public int hashCode() {

				return Objects.hash(idNum, code, indNum);
			}
		}


	}
}
