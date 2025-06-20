/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.isdirty;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.Session;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SessionIsDirtyForNewManyToOneObjectTest extends BaseSessionFactoryFunctionalTest {
	private Long parentId;
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { EntityChild.class, EntityChildAssigned.class, EntityParent.class };
	}
	@BeforeEach
	public void setUp() {
		inTransaction( session -> {
			EntityChild child = new EntityChild("InitialChild");
			EntityChildAssigned assigned = new EntityChildAssigned(1L, "InitialChild");
			EntityParent parent = new EntityParent("InitialParent", child, assigned);
			session.persist(child);
			session.persist(parent);
			session.persist(assigned);
			session.flush();
			parentId = parent.id;
		} );
	}
	@Test
	public void SessionIsDirtyShouldNotFailForNewManyToOneObject()
	{
		inTransaction( session -> {
			var parent = getParent(session);
			EntityChild nextChild = new EntityChild("NewManyToOneChild");
			//parent.Child entity is not cascaded, I want to save it explicitly later
			parent.child = nextChild;
			// will throw TransientObjectException
			assertDoesNotThrow(()->session.isDirty(), "session.isDirty() call should not fail for transient  many-to-one object referenced in session.\"");
			assertTrue(session.isDirty(),"session.isDirty() call should return true.");
			session.persist(nextChild);
		});
	}
	@Test
	public void SessionIsDirtyShouldNotFailForNewManyToOneObjectWithAssignedId()
	{
		inTransaction( session -> {
			var parent = getParent(session);
			EntityChildAssigned nextChildAssigned = new EntityChildAssigned(2L, "NewManyToOneChildAssignedId");
			//parent.ChildAssigned entity is not cascaded, I want to save it explicitly later
			parent.childAssigned = nextChildAssigned;
			assertDoesNotThrow(()->session.isDirty(), "session.isDirty() call should not fail for transient  many-to-one object referenced in session.\"");
			assertTrue(session.isDirty(),"session.isDirty() call should return true.");
			session.persist(nextChildAssigned);
		});
	}
	private EntityParent getParent(Session session){
		return session.get(EntityParent.class, parentId);
	}
	@AfterEach
	public void cleanUp() {
		sessionFactoryScope().getSessionFactory().getSchemaManager().truncate();
	}
	@Entity(name = "EntityChild")
	public static class EntityChild
	{
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		Long id;
		String name;
		EntityChild(){}
		EntityChild(String name){this.name = name;}
	}
	@Entity(name = "EntityParent")
	public static class EntityParent
	{
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		Long id;
		String name;
		@ManyToOne
		EntityChild child;
		@ManyToOne
		EntityChildAssigned childAssigned;
		EntityParent(){}
		EntityParent(String name, EntityChild child, EntityChildAssigned childAssigned){
			this.name = name;
			this.child = child;
			this.childAssigned = childAssigned;
		}
	}
	@Entity(name = "EntityChildAssigned")
	public static class EntityChildAssigned
	{
		@Id
		Long id;
		String name;
		EntityChildAssigned(){}
		EntityChildAssigned(Long id, String name){
			this.id = id;
			this.name = name;
		}
	}
}
