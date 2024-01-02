/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.persister.entity;

import org.hibernate.annotations.Loader;
import org.hibernate.annotations.NamedNativeQuery;
import org.hibernate.annotations.Persister;
import org.hibernate.annotations.ResultCheckStyle;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.sql.model.jdbc.JdbcMutationOperation;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Laabidi RAISSI
 */
@DomainModel(annotatedClasses = {
		CustomSqlSchemaResolvingTest.CustomEntity.class, CustomSqlSchemaResolvingTest.Dummy.class
})
@SessionFactory
public class CustomSqlSchemaResolvingTest {

	@Test
	public void testSchemaNotReplacedInCustomSQL(SessionFactoryScope scope) throws Exception {

		String className = CustomEntity.class.getName();

        final AbstractEntityPersister persister = (AbstractEntityPersister) scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor(className);
		String insertQuery = ( (JdbcMutationOperation) persister.getInsertCoordinator().getStaticMutationOperationGroup().getSingleOperation() ).getSqlString();
		String updateQuery = ( (JdbcMutationOperation) persister.getUpdateCoordinator().getStaticMutationOperationGroup().getSingleOperation() ).getSqlString();
		String deleteQuery = ( (JdbcMutationOperation) persister.getDeleteCoordinator().getStaticMutationOperationGroup().getSingleOperation() ).getSqlString();

		assertEquals( "Incorrect custom SQL for insert in  Entity: " + className,
				"INSERT INTO FOO (name, id) VALUES (?, ?)", insertQuery );
		
		assertEquals( "Incorrect custom SQL for delete in  Entity: " + className,
				"DELETE FROM FOO WHERE id = ?", deleteQuery );
		
		assertEquals( "Incorrect custom SQL for update in  Entity: " + className,
				"UPDATE FOO SET name = ? WHERE id = ?", updateQuery );

		scope.inTransaction( session -> {
			CustomEntity entity = new CustomEntity();
			entity.id = 1;
			session.persist( entity );
		} );

		scope.inTransaction( session -> {
			CustomEntity entity = session.find( CustomEntity.class, 1 );
			assertNotNull(entity);

			entity.name = "Vlad";
		} );

		scope.inTransaction( session -> {
			CustomEntity entity = session.find( CustomEntity.class, 1 );
			session.delete( entity );
		} );

		scope.inTransaction( session -> {
			CustomEntity entity = session.find( CustomEntity.class, 1 );
			assertNull(entity);
		} );
	}

	@Entity(name = "CardWithCustomSQL")
	@Persister( impl = SingleTableEntityPersister.class )
	@Loader(namedQuery = "find_foo_by_id")
	@NamedNativeQuery(
		name = "find_foo_by_id",
		query = "SELECT id, name FROM {h-schema}FOO WHERE id = ?",
		resultClass = CustomEntity.class
	)
	@SQLInsert(sql = "INSERT INTO {h-schema}FOO (name, id) VALUES (?, ?)")
	@SQLDelete(sql = "DELETE FROM {h-schema}FOO WHERE id = ?", check = ResultCheckStyle.COUNT)
	@SQLUpdate(sql = "UPDATE {h-schema}FOO SET name = ? WHERE id = ? ")
	public static class CustomEntity {
		@Id
		public Integer id;

		private String name;
	}
	
	@Entity(name = "Dummy")
	@Table(name = "FOO")
	public static class Dummy {
		@Id
		public Integer id;

		private String name;
	}
}
