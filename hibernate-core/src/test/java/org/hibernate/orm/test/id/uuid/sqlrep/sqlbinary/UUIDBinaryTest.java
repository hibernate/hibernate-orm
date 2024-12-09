/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.id.uuid.sqlrep.sqlbinary;

import java.sql.Types;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = { UUIDBinaryTest.Node.class })
@SessionFactory
@SkipForDialect(dialectClass = PostgreSQLDialect.class, reason = "Postgres has its own UUID type")
@SkipForDialect( dialectClass = SybaseDialect.class, matchSubTypes = true,
		reason = "Skipped for Sybase to avoid problems with UUIDs potentially ending with a trailing 0 byte")
@SkipForDialect(dialectClass = InformixDialect.class, reason = "Informix does not support unique / primary constraints on binary columns")
public class UUIDBinaryTest {

	private static class UUIDPair {
		UUID rootId;
		UUID childId;

		public UUIDPair(UUID rootId, UUID childId) {
			this.rootId = rootId;
			this.childId = childId;
		}
	}

	@Test
	public void testUsage(SessionFactoryScope scope) {
		final MappingMetamodel domainModel = scope.getSessionFactory().getRuntimeMetamodels().getMappingMetamodel();
		final EntityPersister entityDescriptor = domainModel.findEntityDescriptor( Node.class );
		final JdbcMapping jdbcMapping = entityDescriptor.getIdentifierMapping().getSingleJdbcMapping();
		assertThat( jdbcMapping.getJdbcType().isBinary(), is( true ) );

		final UUIDPair uuidPair = scope.fromTransaction( session -> {
			final Node root = new Node( "root" );
			session.save( root );
			assertThat( root.id, notNullValue());

			final Node child = new Node( "child", root );
			session.save( child );
			assertThat( child.id, notNullValue() );

			return new UUIDPair(root.id, child.id);
		} );

		scope.inTransaction( session -> {
			final Node root = session.get( Node.class, uuidPair.rootId );
			assertThat( root, notNullValue() );
			final Node child = session.get( Node.class, uuidPair.childId );
			assertThat( child, notNullValue() );
		} );

		scope.inTransaction( session -> {
			final Node node = session.createQuery( "from Node n join fetch n.parent where n.parent is not null", Node.class ).uniqueResult();
			assertThat( node, notNullValue() );
			assertThat( node.parent, notNullValue() );
		} );
	}

	@Entity(name = "Node")
	static class Node {

		@Id
		@GeneratedValue
		@JdbcTypeCode(Types.BINARY)
		UUID id;

		String name;

		@ManyToOne
		Node parent;

		Node() {
		}

		Node(String name) {
			this.name = name;
		}

		Node(String name, Node parent) {
			this.name = name;
			this.parent = parent;
		}
	}

}
