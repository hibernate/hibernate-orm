package org.hibernate.boot.internal;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.IdentifierCollection;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Value;

/**
 * Releases state which is only needed while binding the boot model, once
 * the {@code SessionFactory} has been created. This makes the annotation
 * binders, the {@link org.hibernate.models.spi.ModelsContext} and all the
 * reflection details it holds unreachable, while keeping the boot model
 * usable for {@linkplain org.hibernate.relational.SchemaManager schema management}.
 * <p>
 * After the release, the boot model can no longer be used to build another
 * {@code SessionFactory}.
 *
 * @see org.hibernate.cfg.PersistenceSettings#RELEASE_BOOT_BINDING_STATE
 */
public final class BindingStateReleaser {

	private final Set<Value> visited = Collections.newSetFromMap( new IdentityHashMap<>() );
	private final Set<InFlightMetadataCollectorImpl> collectors = Collections.newSetFromMap( new IdentityHashMap<>() );

	private BindingStateReleaser() {
	}

	public static void release(MetadataImplementor metadata, BootstrapContext bootstrapContext) {
		final var releaser = new BindingStateReleaser();
		for ( var persistentClass : metadata.getEntityBindings() ) {
			releaser.release( persistentClass );
		}
		for ( var collection : metadata.getCollectionBindings() ) {
			releaser.release( collection );
		}
		metadata.visitRegisteredComponents( releaser::release );
		for ( var namespace : metadata.getDatabase().getNamespaces() ) {
			for ( var table : namespace.getTables() ) {
				for ( var column : table.getColumns() ) {
					releaser.release( column.getValue() );
				}
			}
		}

		for ( var collector : releaser.collectors ) {
			collector.releaseBindingState();
		}
		if ( bootstrapContext instanceof BootstrapContextImpl bootstrapContextImpl ) {
			bootstrapContextImpl.releaseModelsContext();
		}
	}

	private void release(PersistentClass persistentClass) {
		if ( persistentClass instanceof RootClass rootClass ) {
			rootClass.setNaturalIdClass( null );
		}
		release( persistentClass.getIdentifier() );
		release( persistentClass.getIdentifierMapper() );
		release( persistentClass.getDiscriminator() );
		release( persistentClass.getKey() );
		for ( var property : persistentClass.getProperties() ) {
			release( property );
		}
		for ( var join : persistentClass.getJoins() ) {
			release( join );
		}
	}

	private void release(Join join) {
		release( join.getKey() );
		for ( var property : join.getProperties() ) {
			release( property );
		}
	}

	private void release(Property property) {
		property.releaseBindingState();
		release( property.getValue() );
	}

	private void release(Value value) {
		if ( value != null && visited.add( value ) ) {
			if ( value instanceof SimpleValue simpleValue ) {
				simpleValue.releaseBindingState();
				if ( simpleValue.getBuildingContext().getMetadataCollector()
						instanceof InFlightMetadataCollectorImpl collector ) {
					collectors.add( collector );
				}
			}
			if ( value instanceof Any any ) {
				release( any.getDiscriminatorDescriptor() );
				release( any.getKeyDescriptor() );
			}
			else if ( value instanceof Component component ) {
				release( component.getDiscriminator() );
				for ( var property : component.getProperties() ) {
					release( property );
				}
			}
			else if ( value instanceof Collection collection ) {
				release( collection.getKey() );
				release( collection.getElement() );
				if ( collection instanceof IndexedCollection indexedCollection ) {
					release( indexedCollection.getIndex() );
				}
				if ( collection instanceof IdentifierCollection identifierCollection ) {
					release( identifierCollection.getIdentifier() );
				}
			}
		}
	}
}
