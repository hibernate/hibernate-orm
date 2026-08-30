/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
		// GaussDB M mode maps BINARY to MySQL `binary($l)` (dolphin extension; M mode rejects PG `bytea`).
		// gsjdbc4 setBytes() sends a bytea oid that the M-mode binary column accepts for `insert ... values(?)`
		// (the SharedDriverManagerConnectionProvider patches TypeInfoCache to infer the param type from the
		// column) but REJECTS for a `where id=?` comparison param — "invalid byte sequence for encoding UTF8"
		// (bytea->binary has no implicit cast), and `cast(? as binary)`/`?::binary` cannot work around it
		// (any-typed / length-doubled "exceeds 16"). This is a deep gsjdbc4 + M-mode-binary driver limitation,
		// not dialect-fixable (BINARY sqlType must map to a binary column; see tasks/lessons.md "P6 遗留").
		// A mode (PG kernel) uses bytea and setBytes/getBytes work, so M-only skip.
		org.junit.jupiter.api.Assumptions.assumeFalse( scope.getSessionFactory().getJdbcServices().getDialect() instanceof org.hibernate.community.dialect.GaussDBDialect g && g.isMMode() );
		final MappingMetamodel domainModel = scope.getSessionFactory().getRuntimeMetamodels().getMappingMetamodel();
		final EntityPersister entityDescriptor = domainModel.findEntityDescriptor( Node.class );
		final JdbcMapping jdbcMapping = entityDescriptor.getIdentifierMapping().getSingleJdbcMapping();
		assertThat( jdbcMapping.getJdbcType().isBinary(), is( true ) );

		final UUIDPair uuidPair = scope.fromTransaction( session -> {
			final Node root = new Node( "root" );
			session.persist( root );
			assertThat( root.id, notNullValue());

			final Node child = new Node( "child", root );
			session.persist( child );
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
