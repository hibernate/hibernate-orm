/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.component.empty;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.Parent;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.CompositeType;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
public class EmptyCompositeEquivalentToNullTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { AnEntity.class };
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.getProperties().put( Environment.CREATE_EMPTY_COMPOSITES_ENABLED, "true" );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11898" )
	@FailureExpected( jiraKey = "HHH-11898" )
	public void testPrimitive() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					AnEntity anEntity = new AnEntity();
					session.persist( anEntity );
					session.flush();
					session.clear();
					anEntity = session.get( AnEntity.class, anEntity.id );
					checkEmptyCompositeTypeEquivalentToNull(
							anEntity.embeddableWithPrimitive,
							"embeddableWithPrimitive",
							sessionFactory()
					);
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11898" )
	public void testParent() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					AnEntity anEntity = new AnEntity();
		 			session.persist( anEntity );
					session.flush();
					session.clear();
					anEntity = session.get( AnEntity.class, anEntity.id );
					checkEmptyCompositeTypeEquivalentToNull(
							anEntity.embeddableWithParent,
							"embeddableWithParent",
							sessionFactory()
					);
				}
		);

	}

	@Test
	@TestForIssue( jiraKey = "HHH-11898" )
	public void testNoPrimitiveNoParent() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					AnEntity anEntity = new AnEntity();
					session.persist( anEntity );
					session.flush();
					session.clear();
					anEntity = session.get( AnEntity.class, anEntity.id );
					checkEmptyCompositeTypeEquivalentToNull(
							anEntity.embeddableWithNoPrimitiveNoParent,
							"embeddableWithNoPrimitiveNoParent",
							sessionFactory()
					);
				}
		);
	}

	private void checkEmptyCompositeTypeEquivalentToNull(
			Object compositeValue,
			String componentPropertyName,
			SessionFactoryImplementor sessionFactory) {
		assertNotNull( compositeValue );
		final CompositeType compositeType = getCompositeType( componentPropertyName, sessionFactory );
		assertTrue( compositeType.isEqual( null, compositeValue, sessionFactory ) );
		assertTrue( compositeType.isEqual( compositeValue, null, sessionFactory ) );
	}

	private CompositeType getCompositeType(String componentPropertyName, SessionFactoryImplementor sessionFactory) {
		return (CompositeType) sessionFactory
				.getMetamodel()
				.entityPersister( AnEntity.class )
				.getPropertyType( componentPropertyName );
	}

	@Entity(name = "AnEntity")
	public static class AnEntity {
		private int id;
		private EmbeddableWithParent embeddableWithParent;
		private EmbeddableWithPrimitive embeddableWithPrimitive;
		private EmbeddableWithNoPrimitiveNoParent embeddableWithNoPrimitiveNoParent;

		@Id
		@GeneratedValue
		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}

		public EmbeddableWithParent getEmbeddableWithParent() {
			return embeddableWithParent;
		}
		public void setEmbeddableWithParent(EmbeddableWithParent embeddableWithParent) {
			this.embeddableWithParent = embeddableWithParent;
		}

		public EmbeddableWithPrimitive getEmbeddableWithPrimitive() {
			return embeddableWithPrimitive;
		}
		public void setEmbeddableWithPrimitive(EmbeddableWithPrimitive embeddableWithPrimitive) {
			this.embeddableWithPrimitive = embeddableWithPrimitive;
		}

		public EmbeddableWithNoPrimitiveNoParent getEmbeddableWithNoPrimitiveNoParent() {
			return embeddableWithNoPrimitiveNoParent;
		}
		public void setEmbeddableWithNoPrimitiveNoParent(EmbeddableWithNoPrimitiveNoParent embeddableWithNoPrimitiveNoParent) {
			this.embeddableWithNoPrimitiveNoParent = embeddableWithNoPrimitiveNoParent;
		}
	}

	@Embeddable
	public static class EmbeddableWithParent {
		private Object parent;
		private Long longObjectValue;

		@Parent
		public Object getParent() {
			return parent;
		}
		public void setParent(Object parent) {
			this.parent = parent;
		}

		public Long getLongObjectValue() {
			return longObjectValue;
		}
		public void setLongObjectValue(Long longObjectValue) {
			this.longObjectValue = longObjectValue;
		}
	}

	@Embeddable
	public static class EmbeddableWithPrimitive {
		private int intValue;
		private String stringValue;

		@Column(nullable = true)
		public int getIntValue() {
			return intValue;
		}
		public void setIntValue(int intValue) {
			this.intValue = intValue;
		}

		public String getStringValue() {
			return stringValue;
		}
		public void setStringValue(String stringValue) {
			this.stringValue = stringValue;
		}

	}

	@Embeddable
	public static class EmbeddableWithNoPrimitiveNoParent {
		private Integer intObjectValue;
		private String otherStringValue;

		public Integer getIntObjectValue() {
			return intObjectValue;
		}
		public void setIntObjectValue(Integer intObjectValue) {
			this.intObjectValue = intObjectValue;
		}

		public String getOtherStringValue() {
			return otherStringValue;
		}
		public void setOtherStringValue(String otherStringValue) {
			this.otherStringValue = otherStringValue;
		}
	}
}
