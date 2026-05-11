/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.action.queue.proof;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.cfg.BatchSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Normunds Gavars
 * @author Steve Ebersole
 */
@JiraKey("HHH-14344")
// MySQLDialect and a few others do not enable batching by default
@ServiceRegistry(settings = @Setting(name= BatchSettings.STATEMENT_BATCH_SIZE, value="15"))
@DomainModel(annotatedClasses = {
		InsertOrderingReferenceSeveralDifferentSubclassTest.BaseEntity.class,
		InsertOrderingReferenceSeveralDifferentSubclassTest.SubclassZero.class,
		InsertOrderingReferenceSeveralDifferentSubclassTest.SubclassOne.class,
		InsertOrderingReferenceSeveralDifferentSubclassTest.SubclassTwo.class,
		InsertOrderingReferenceSeveralDifferentSubclassTest.UnrelatedEntity.class
})
@SessionFactory(useCollectingStatementObserver = true)
public class InsertOrderingReferenceSeveralDifferentSubclassTest {

	@Test
	public void testSubclassReferenceChain(SessionFactoryScope factoryScope) {
		var sqlCollector = factoryScope.getCollectingStatementObserver();

		factoryScope.inTransaction( (session) -> {
			UnrelatedEntity unrelatedEntity1 = new UnrelatedEntity();
			session.persist(unrelatedEntity1);

			SubclassZero subclassZero = new SubclassZero();
			session.persist(subclassZero);
			SubclassOne subclassOne = new SubclassOne();
			subclassOne.setParent(subclassZero);
			SubclassTwo subclassTwo = new SubclassTwo();
			subclassTwo.setParent(subclassOne);

			session.persist(subclassTwo);
			session.persist(subclassOne);

			// add extra instances for the sake of volume
			UnrelatedEntity unrelatedEntity2 = new UnrelatedEntity();
			session.persist(unrelatedEntity2);
			SubclassOne subclassOne2 = new SubclassOne();
			SubclassTwo subclassD2 = new SubclassTwo();
			session.persist(subclassOne2);
			session.persist(subclassD2);

			sqlCollector.clear();
		} );

//		verifyContainsBatches(
//				new Batch( "insert into UnrelatedEntity (unrelatedValue, id) values (?, ?)", 2 ),
//				new Batch( "insert into BaseEntity (TYPE, id) values ('ZERO', ?)"),
//				new Batch( "insert into BaseEntity (PARENT_ID, TYPE, id) values (?, 'TWO', ?)", 2 ),
//				new Batch( "insert into BaseEntity (PARENT_ID, TYPE, id) values (?, 'ONE', ?)", 2 )
//		);

		// Batch #1
		assertThat( sqlCollector.getStatements().get( 0 ).sql() ).isEqualTo( "insert into UnrelatedEntity (unrelatedValue,id) values (?,?)" );
		assertThat( sqlCollector.getStatements().get( 0 ).batchPosition() ).isEqualTo(1 );
		assertThat( sqlCollector.getStatements().get( 1 ).sql() ).isEqualTo( "insert into UnrelatedEntity (unrelatedValue,id) values (?,?)" );
		assertThat( sqlCollector.getStatements().get( 1 ).batchPosition() ).isEqualTo(2 );

		// Batch #2
		assertThat( sqlCollector.getStatements().get( 2 ).sql() ).isEqualTo( "insert into BaseEntity (e_type,id) values (?,?)" );
		assertThat( sqlCollector.getStatements().get( 2 ).batchPosition() ).isEqualTo(1 );

		// Batch #3
		assertThat( sqlCollector.getStatements().get( 3 ).sql() ).isEqualTo( "insert into BaseEntity (parent_fk,e_type,id) values (?,?,?)" );
		assertThat( sqlCollector.getStatements().get( 3 ).batchPosition() ).isEqualTo(1 );
		assertThat( sqlCollector.getStatements().get( 4 ).sql() ).isEqualTo( "insert into BaseEntity (parent_fk,e_type,id) values (?,?,?)" );
		assertThat( sqlCollector.getStatements().get( 4 ).batchPosition() ).isEqualTo(2 );
		assertThat( sqlCollector.getStatements().get( 5 ).sql() ).isEqualTo( "insert into BaseEntity (parent_fk,e_type,id) values (?,?,?)" );
		assertThat( sqlCollector.getStatements().get( 5 ).batchPosition() ).isEqualTo(3 );
		assertThat( sqlCollector.getStatements().get( 6 ).sql() ).isEqualTo( "insert into BaseEntity (parent_fk,e_type,id) values (?,?,?)" );
		assertThat( sqlCollector.getStatements().get( 6 ).batchPosition() ).isEqualTo(4 );

		factoryScope.inTransaction( (session) -> {
			var unrelatedEntities = session.createQuery( "from UnrelatedEntity", UnrelatedEntity.class ).list();
			assertThat( unrelatedEntities ).hasSize( 2 );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Entity(name = "BaseEntity")
	@DiscriminatorColumn(name = "e_type")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static abstract class BaseEntity {

		@Id
		@GeneratedValue
		private Long id;
	}

	@Entity(name = "SubclassZero")
	@DiscriminatorValue("ZERO")
	public static class SubclassZero extends BaseEntity {

	}

	@Entity(name = "SubclassOne")
	@DiscriminatorValue("ONE")
	public static class SubclassOne extends BaseEntity {

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "parent_fk")
		private SubclassZero parent;

		@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST, orphanRemoval = true, mappedBy = "parent", targetEntity = SubclassTwo.class)
		private List<SubclassTwo> subclassTwoList = new ArrayList<>();

		public void setParent(SubclassZero parent) {
			this.parent = parent;
		}
	}

	@Entity(name ="SubclassTwo")
	@DiscriminatorValue("TWO")
	public static class SubclassTwo extends BaseEntity {

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "parent_fk")
		private SubclassOne parent;

		public void setParent(SubclassOne parent) {
			this.parent = parent;
		}
	}

	@Entity(name = "UnrelatedEntity")
	public static class UnrelatedEntity {
		@Id
		@GeneratedValue
		private Long id;

		private String unrelatedValue;

		public void setUnrelatedValue(String unrelatedValue) {
			this.unrelatedValue = unrelatedValue;
		}
	}
}
