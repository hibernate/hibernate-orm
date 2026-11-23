package org.hibernate.persister.collection;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.mapping.Collection;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.internal.StandardPersisterClassResolver;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.persister.internal.PersisterClassResolverInitiator.IMPL_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DomainModel(annotatedClasses = CollectionScalarSqlOverrideTest.Owner.class)
@ServiceRegistry(settings = @Setting(name = IMPL_NAME,
		value = "org.hibernate.persister.collection.CollectionScalarSqlOverrideTest$Resolver"))
@SessionFactory
class CollectionScalarSqlOverrideTest {
	@Test
	void customStatementsAreUsed(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var owner = new Owner();
			owner.items.add( "item" );
			session.persist( owner );
		} );
		scope.inSession( session -> {
			session.enableFilter( "visible" );
			var items = session.find( Owner.class, 1 ).items;
			assertEquals( 0, Hibernate.size( items ) );
			assertFalse( Hibernate.contains( items, "item" ) );
			final var descriptor = scope.getSessionFactory().getMappingMetamodel()
					.getCollectionDescriptor( Owner.class.getName() + ".items" );
			assertFalse( descriptor.indexExists( 1, 0, session ) );
		} );
	}

	@Entity(name = "ScalarSqlOwner")
	@FilterDef(name = "visible")
	static class Owner {
		@Id int id = 1;
		@ElementCollection @OrderColumn @Filter(name = "visible", condition = "1=1")
		List<String> items = new ArrayList<>();
	}

	public static class Resolver extends StandardPersisterClassResolver {
		@Override
		public Class<? extends CollectionPersister> getCollectionPersisterClass(Collection metadata) {
			return CustomPersister.class;
		}
	}

	public static class CustomPersister extends BasicCollectionPersister {
		public CustomPersister(Collection collection, CollectionDataAccess cache, RuntimeModelCreationContext context) {
			super( collection, cache, context );
		}

		@Override
		protected String generateSelectSizeString(boolean integerIndexed) {
			return super.generateSelectSizeString( integerIndexed ) + " and 1=0";
		}

		@Override
		protected String generateDetectRowByIndexString() {
			return super.generateDetectRowByIndexString() + " and 1=0";
		}

		@Override
		protected String generateDetectRowByElementString() {
			return super.generateDetectRowByElementString() + " and 1=0";
		}
	}
}
