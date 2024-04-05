/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.LazyGroup;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

@JiraKey( "HHH-11147" )
@DomainModel(
		annotatedClasses = {
				BidirectionalProxyTest.BEntity.class,
				BidirectionalProxyTest.CEntity.class,
				BidirectionalProxyTest.AMappedSuperclass.class,
				BidirectionalProxyTest.AEntity.class,
				BidirectionalProxyTest.AChildEntity.class
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
public class BidirectionalProxyTest {

	@Test
	public void testIt(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					for (BEntity b : session.createQuery("from BEntity b", BEntity.class).getResultList()) {
						final Statistics stats = scope.getSessionFactory().getStatistics();
						stats.clear();
						AChildEntity a = b.getA();
						assertEquals( 0, stats.getPrepareStatementCount() );
						a.getVersion();
						assertEquals( 1, stats.getPrepareStatementCount() );
						a.getStringField();
						assertEquals( 1, stats.getPrepareStatementCount() );
						a.getIntegerField();
						assertEquals( 1, stats.getPrepareStatementCount() );
						a.setIntegerField( 1 );
						assertEquals( 1, stats.getPrepareStatementCount() );
						a.getEntries();
						assertEquals( 1, stats.getPrepareStatementCount() );
						a.setVersion( new Short( "2" ) );
						assertEquals( 1, stats.getPrepareStatementCount() );
						a.setStringField( "this is a string" );
						assertEquals( 1, stats.getPrepareStatementCount() );

						AMappedSuperclass mappedSuperclass = a;
						mappedSuperclass.getVersion();
						assertEquals( 1, stats.getPrepareStatementCount() );
					}
				}
		);

		scope.inTransaction(
				session -> {
					for (BEntity b : session.createQuery("from BEntity b", BEntity.class).getResultList()) {
						final Statistics stats = scope.getSessionFactory().getStatistics();
						stats.clear();
						AChildEntity a = b.getA();
						assertEquals( "this is a string", a.getStringField() );
						assertEquals( 2, a.getVersion() );
						assertEquals( new Integer( 1 ), a.getIntegerField() );
					}
				}
		);

		scope.inTransaction(
				session -> {
					for (CEntity c : session.createQuery("from CEntity c", CEntity.class).getResultList()) {
						final Statistics stats = scope.getSessionFactory().getStatistics();
						stats.clear();
						AEntity a = c.getA();
						assertEquals( 0, stats.getPrepareStatementCount() );
						a.getVersion();
						assertEquals( 1, stats.getPrepareStatementCount() );
						a.getIntegerField();
						assertEquals( 1, stats.getPrepareStatementCount() );
						a.setIntegerField( 1 );
						assertEquals( 1, stats.getPrepareStatementCount() );
						a.setVersion( new Short( "2" ) );
						assertEquals( 1, stats.getPrepareStatementCount() );
					}
				}
		);

		scope.inTransaction(
				session -> {
					for (CEntity c : session.createQuery("from CEntity c", CEntity.class).getResultList()) {
						final Statistics stats = scope.getSessionFactory().getStatistics();
						stats.clear();
						AEntity a = c.getA();
						assertEquals( 2, a.getVersion() );
						assertEquals( new Integer( 1 ), a.getIntegerField() );
					}
				}
		);
	}

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					AChildEntity a = new AChildEntity("a");
					BEntity b = new BEntity("b");
					b.setA(a);
					session.persist(a);
					session.persist(b);

					AChildEntity a1 = new AChildEntity("a1");
					CEntity c = new CEntity( "c" );
					c.setA( a1 );
					session.persist( a1 );
					session.persist( c );
				}
		);
	}

	@AfterEach
	public void clearTestData(SessionFactoryScope scope){
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from BEntity" ).executeUpdate();
					session.createQuery( "delete from CEntity" ).executeUpdate();
					session.createQuery( "delete from AEntity" ).executeUpdate();
				}
		);
	}

	@Entity(name="CEntity")
	@Table(name="C")
	public static class CEntity implements Serializable {
		@Id
		private String id;

		public CEntity(String id) {
			this();
			setId(id);
		}

		protected CEntity() {
		}

		public String getId() {
			return id;
		}

		protected void setId(String id) {
			this.id = id;
		}

		public void setA(AEntity a) {
			aChildEntity = a;
			a.getcEntries().add(this);
		}

		public AEntity getA() {
			return aChildEntity;
		}

		@ManyToOne(fetch= FetchType.LAZY)
		@LazyGroup("aEntity")
		@JoinColumn(name="aEntity")
		protected AEntity aChildEntity = null;
	}

	@Entity(name="BEntity")
	@Table(name="B")
	public static class BEntity implements Serializable {
		@Id
		private String id;

		public BEntity(String id) {
			this();
			setId(id);
		}

		protected BEntity() {
		}

		public String getId() {
			return id;
		}

		protected void setId(String id) {
			this.id = id;
		}

		public void setA(AChildEntity a) {
			aChildEntity = a;
			a.getEntries().add(this);
		}

		public AChildEntity getA() {
			return aChildEntity;
		}

		@ManyToOne(fetch= FetchType.LAZY)
		@LazyGroup("aEntity")
		@JoinColumn(name="aEntity")
		protected AChildEntity aChildEntity = null;
	}

	@MappedSuperclass
	public static class AMappedSuperclass implements Serializable {

		@Id
		private String id;

		@Basic
		private short version;

		@Column(name = "INTEGER_FIELD")
		private Integer integerField;

		public AMappedSuperclass(String id) {
			setId(id);
		}

		protected AMappedSuperclass() {
		}

		public String getId() {
			return id;
		}

		protected void setId(String id) {
			this.id = id;
		}

		public short getVersion() {
			return version;
		}

		public void setVersion(short version) {
			this.version = version;
		}

		public Integer getIntegerField() {
			return integerField;
		}

		public void setIntegerField(Integer integerField) {
			this.integerField = integerField;
		}
	}

	@Entity(name="AEntity")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	@Table(name="A")
	public static class AEntity extends AMappedSuperclass {

		public AEntity(String id) {
			super(id);
		}

		protected AEntity() {
		}

		@OneToMany(targetEntity=CEntity.class, mappedBy="aChildEntity", fetch=FetchType.LAZY)
		protected Set<CEntity> cEntries = new LinkedHashSet();

		public Set<CEntity> getcEntries() {
			return cEntries;
		}

		public void setcEntries(Set<CEntity> cEntries) {
			this.cEntries = cEntries;
		}
	}

	@Entity(name="AChildEntity")
	@Table(name="ACChild")
	public static class AChildEntity extends AEntity {

		private String stringField;

		@OneToMany(targetEntity=BEntity.class, mappedBy="aChildEntity", fetch=FetchType.LAZY)
		protected Set<BEntity> entries = new LinkedHashSet();

		public AChildEntity(String id) {
			super(id);
		}

		protected AChildEntity() {
		}

		public Set<BEntity> getEntries() {
			return entries;
		}

		public String getStringField() {
			return stringField;
		}

		public void setStringField(String stringField) {
			this.stringField = stringField;
		}

		@Override
		public Integer getIntegerField() {
			return super.getIntegerField();
		}

		@Override
		public void setIntegerField(Integer integerField) {
			super.setIntegerField( integerField );
		}
	}
}
