package org.hibernate.orm.test.bytecode.enhancement.bidirectional;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				BidirectionalAssociationManagementTest.Parent.class,
				BidirectionalAssociationManagementTest.Child.class
		}
)
@SessionFactory(statementInspectorClass = SQLStatementInspector.class)
@BytecodeEnhanced
@EnhancementOptions(lazyLoading = true, inlineDirtyChecking = true, biDirectionalAssociationManagement = true)
@JiraKey("HHH-18557")
public class BidirectionalAssociationManagementTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Child child1 = new Child( 1 );
					Child child2 = new Child( 2 );

					Parent parent1 = new Parent( 1 );
					session.persist( parent1 );

					parent1.addChild( child1 );
					parent1.addChild( child2 );

					session.persist( child1 );
					session.persist( child2 );

					Parent parent2 = new Parent( 2 );
					session.persist( parent2 );

					Child child3 = new Child( 3 );
					parent2.addChild( child3 );
					session.persist( child3 );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope){
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "delete from Child" ).executeUpdate();
					session.createMutationQuery( "delete from Parent" ).executeUpdate();
				}
		);
	}

	@Test
	public void testParentCollectionIsNotInitialized(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getStatementInspector( SQLStatementInspector.class );
		scope.inTransaction(
				session -> {
					Child child = session.get( Child.class, 2 );
					Parent newParent = session.get( Parent.class, 2 );

					statementInspector.clear();
					child.setParent( newParent );
					statementInspector.assertExecutedCount( 1 );
					Parent oldParent = session.get( Parent.class, 1 );

					assertThat( Hibernate.isInitialized( newParent.getChildren() ) ).isFalse();
					assertThat( Hibernate.isInitialized( oldParent.getChildren() ) ).isFalse();
					statementInspector.clear();
				}
		);
		assertThat( statementInspector.getSqlQueries().size() ).isEqualTo( 1 );
		statementInspector.assertIsUpdate( 0 );
	}

	@Test
	public void testCollectionsHaveRightSize(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getStatementInspector( SQLStatementInspector.class );
		scope.inTransaction(
				session -> {
					Child child = session.get( Child.class, 2 );
					Parent newParent = session.get( Parent.class, 2 );

					statementInspector.clear();
					child.setParent( newParent );
					statementInspector.assertExecutedCount( 1 );
					Parent oldParent = session.get( Parent.class, 1 );

					assertThat( Hibernate.isInitialized( newParent.getChildren() ) ).isFalse();
					assertThat( Hibernate.isInitialized( oldParent.getChildren() ) ).isFalse();

					assertThat( newParent.getChildren().size() ).isEqualTo( 2 );
					assertThat( oldParent.getChildren().size() ).isEqualTo( 1 );
					statementInspector.clear();
				}
		);
		assertThat( statementInspector.getSqlQueries().size() ).isEqualTo( 1 );
		statementInspector.assertIsUpdate( 0 );
	}

	@Entity(name = "Parent")
	public static class Parent {

		@Id
		private int id;

		@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
		private List<Child> children = new ArrayList<>();

		public Parent() {
		}

		public Parent(int id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}

		public List<Child> getChildren() {
			return children;
		}

		public void addChild(Child operation) {
			children.add( operation );
			operation.setParent( this );
		}
	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		private int id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumns({ @JoinColumn(name = "parentId") })
		private Parent parent;

		public Child() {
		}

		public Child(int id) {
			this.id = id;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}
}
