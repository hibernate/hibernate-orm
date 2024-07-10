/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.graalvm.internal;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.graph.internal.parse.SubGraphGenerator;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.query.hql.spi.DotIdentifierConsumer;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.sqm.spi.ParameterDeclarationContext;
import org.hibernate.query.sqm.sql.FromClauseIndex;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.SqlAstProcessingState;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingState;
import org.hibernate.tool.schema.internal.script.MultiLineSqlScriptExtractor;
import org.hibernate.type.EnumType;

/**
 * The place to list all "static" types we know of that need to be possible to
 * construct at runtime via reflection.
 * This is useful for GraalVM native images - but is not intended to be an
 * exhaustive list: take these as a helpful starting point.
 */
final class StaticClassLists {

	public static Class[] typesNeedingAllConstructorsAccessible() {
		return new Class[] {
				//The CoreMessageLogger is sometimes looked up without it necessarily being a field, so we're
				//not processing it the same way as other Logger lookups.
				org.hibernate.internal.CoreMessageLogger_$logger.class,
				org.hibernate.persister.collection.OneToManyPersister.class,
				org.hibernate.persister.collection.BasicCollectionPersister.class,
				org.hibernate.persister.entity.JoinedSubclassEntityPersister.class,
				org.hibernate.persister.entity.UnionSubclassEntityPersister.class,
				org.hibernate.persister.entity.SingleTableEntityPersister.class,
		};
	}

	public static Class[] typesNeedingDefaultConstructorAccessible() {
		return new Class[] {
				//Various well known needs:
				org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl.class,
				org.hibernate.id.enhanced.SequenceStyleGenerator.class,
				org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl.class,
				org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl.class,
				EnumType.class,
				MultiLineSqlScriptExtractor.class,

		};
	}

	public static Class[] typesNeedingArrayCopy() {
		return new Class[] {
				//Eventlisteners need to be registered for reflection to allow creation via Array#newInstance ;
				// types need to be in synch with those declared in org.hibernate.event.spi.EventType
				org.hibernate.event.spi.LoadEventListener[].class,
				org.hibernate.event.spi.ResolveNaturalIdEventListener[].class,
				org.hibernate.event.spi.InitializeCollectionEventListener[].class,
				org.hibernate.event.spi.SaveOrUpdateEventListener[].class,
				org.hibernate.event.spi.PersistEventListener[].class,
				org.hibernate.event.spi.MergeEventListener[].class,
				org.hibernate.event.spi.DeleteEventListener[].class,
				org.hibernate.event.spi.ReplicateEventListener[].class,
				org.hibernate.event.spi.FlushEventListener[].class,
				org.hibernate.event.spi.AutoFlushEventListener[].class,
				org.hibernate.event.spi.DirtyCheckEventListener[].class,
				org.hibernate.event.spi.FlushEntityEventListener[].class,
				org.hibernate.event.spi.ClearEventListener[].class,
				org.hibernate.event.spi.EvictEventListener[].class,
				org.hibernate.event.spi.LockEventListener[].class,
				org.hibernate.event.spi.RefreshEventListener[].class,
				org.hibernate.event.spi.PreLoadEventListener[].class,
				org.hibernate.event.spi.PreDeleteEventListener[].class,
				org.hibernate.event.spi.PreUpdateEventListener[].class,
				org.hibernate.event.spi.PreUpsertEventListener[].class,
				org.hibernate.event.spi.PreInsertEventListener[].class,
				org.hibernate.event.spi.PostLoadEventListener[].class,
				org.hibernate.event.spi.PostDeleteEventListener[].class,
				org.hibernate.event.spi.PostUpdateEventListener[].class,
				org.hibernate.event.spi.PostUpsertEventListener[].class,
				org.hibernate.event.spi.PostInsertEventListener[].class,
				org.hibernate.event.spi.PreCollectionRecreateEventListener[].class,
				org.hibernate.event.spi.PreCollectionRemoveEventListener[].class,
				org.hibernate.event.spi.PreCollectionUpdateEventListener[].class,
				org.hibernate.event.spi.PostCollectionRecreateEventListener[].class,
				org.hibernate.event.spi.PostCollectionRemoveEventListener[].class,
				org.hibernate.event.spi.PostCollectionUpdateEventListener[].class,
				//And other array types, necessary for allocation of generified instances of org.hibernate.internal.util.collections.StandardStack:
				//TODO can this list be tested for consistency with the core module? Or generated? e.g. could use Jandex?
				AttributeNodeImplementor[].class,
				Clause[].class,
				DotIdentifierConsumer[].class,
				FetchParent[].class,
				FromClauseIndex[].class,
				Function[].class,
				GraphImplementor[].class,
				JdbcValuesSourceProcessingState[].class,
				List[].class,
				Map.Entry[].class,
				ParameterDeclarationContext[].class,
				QueryPart[].class,
				SqlAstProcessingState[].class,
				SqmCreationProcessingState[].class,
				Statement[].class,
				SubGraphGenerator[].class,
				Supplier[].class,
		};
	}

}
