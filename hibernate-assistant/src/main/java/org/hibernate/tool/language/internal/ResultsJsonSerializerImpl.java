/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.language.internal;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.spi.SqmQuery;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmExpressibleAccessor;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.select.SqmJpaCompoundSelection;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.tool.language.spi.ResultsSerializer;
import org.hibernate.type.descriptor.jdbc.spi.DescriptiveJsonGeneratingVisitor;
import org.hibernate.type.format.StringJsonDocumentWriter;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.Selection;
import java.io.IOException;
import java.util.List;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * Utility class to serialize query results into a JSON string format.
 */
public class ResultsJsonSerializerImpl implements ResultsSerializer {

	private static final DescriptiveJsonGeneratingVisitor JSON_VISITOR = new DescriptiveJsonGeneratingVisitor();

	private final SessionFactoryImplementor factory;

	public ResultsJsonSerializerImpl(SessionFactoryImplementor factory) {
		this.factory = factory;
	}

	@Override
	public <T> String toString(List<? extends T> values, SelectionQuery<T> query) throws IOException {
		if ( values.isEmpty() ) {
			return "[]";
		}

		final StringBuilder sb = new StringBuilder();
		final StringJsonDocumentWriter writer = new StringJsonDocumentWriter( sb );
		char separator = '[';
		for ( final T value : values ) {
			sb.append( separator );
			//noinspection unchecked
			renderValue( value, (SqmQuery<? super T>) query, writer );
			separator = ',';
		}
		sb.append( ']' );
		return sb.toString();
	}

	private <T> void renderValue(T value, SqmQuery<? super T> query, StringJsonDocumentWriter writer)
			throws IOException {
		final SqmStatement<?> sqm = query.getSqmStatement();
		if ( !( sqm instanceof SqmSelectStatement<?> sqmSelect ) ) {
			throw new IllegalArgumentException( "Query is not a select statement." );
		}
		final List<SqmSelection<?>> selections = sqmSelect.getQuerySpec().getSelectClause().getSelections();
		assert !selections.isEmpty();
		if ( selections.size() == 1 ) {
			renderValue( value, selections.get( 0 ).getSelectableNode(), writer );
		}
		else {
			// wrap each result tuple in square brackets
			writer.startArray();
			for ( int i = 0; i < selections.size(); i++ ) {
				final SqmSelection<?> selection = selections.get( i );
				if ( value instanceof Object[] array ) {
					renderValue( array[i], selection.getSelectableNode(), writer );
				}
				else if ( value instanceof Tuple tuple ) {
					renderValue( tuple.get( i ), selection.getSelectableNode(), writer );
				}
				else {
					renderValue( value, selection.getSelectableNode(), writer );
				}
			}
			writer.endArray();
		}
	}

	private void renderValue(Object value, Selection<?> selection, StringJsonDocumentWriter writer) throws IOException {
		if ( selection instanceof SqmRoot<?> root ) {
			final EntityPersister persister = factory.getMappingMetamodel()
					.getEntityDescriptor( root.getEntityName() );
			JSON_VISITOR.visit( persister.getEntityMappingType(), value, factory.getWrapperOptions(), writer );
		}
		else if ( selection instanceof SqmPath<?> path ) {
			// extract the attribute from the path
			final ValuedModelPart subPart = getSubPart( path.getLhs(), path.getNavigablePath().getLocalName() );
			if ( subPart != null ) {
				JSON_VISITOR.visit( subPart.getMappedType(), value, factory.getWrapperOptions(), writer );
			}
			else {
				expressibleToString( path, value, writer );
			}
		}
		else if ( selection instanceof SqmJpaCompoundSelection<?> compoundSelection ) {
			final List<Selection<?>> compoundSelectionItems = compoundSelection.getCompoundSelectionItems();
			assert compoundSelectionItems.size() > 1;
			writer.startArray();
			for ( int j = 0; j < compoundSelectionItems.size(); j++ ) {
				renderValue( getValue( value, j ), compoundSelectionItems.get( j ), writer );
			}
			writer.endArray();
		}
		else if ( selection instanceof SqmExpressibleAccessor<?> node ) {
			expressibleToString( node, value, writer );
		}
		else {
			writer.stringValue( String.valueOf( value ) );
		}
	}

	private static void expressibleToString(
			SqmExpressibleAccessor<?> node,
			Object value,
			StringJsonDocumentWriter writer) {
		//noinspection unchecked
		final SqmExpressible<Object> expressible = (SqmExpressible<Object>) node.getExpressible();
		final String result = expressible != null ?
				expressible.getExpressibleJavaType().toString( value ) :
				value.toString(); // best effort
		// avoid quoting numeric and boolean values as they can be represented in JSON
		if ( value instanceof Boolean boolValue ) {
			writer.booleanValue( boolValue );
		}
		else if ( value instanceof Number numValue ) {
			writer.numericValue( numValue );
		}
		else if ( result == null ) {
			writer.nullValue();
		}
		else {
			writer.stringValue( result );
		}
	}

	private static Object getValue(Object value, int index) {
		if ( value.getClass().isArray() ) {
			return ( (Object[]) value )[index];
		}
		else if ( value instanceof Tuple tuple ) {
			return tuple.get( index );
		}
		else {
			if ( index > 0 ) {
				throw new IllegalArgumentException( "Index out of range: " + index );
			}
			return value;
		}
	}

	private ValuedModelPart getSubPart(SqmPath<?> path, String propertyName) {
		if ( path instanceof SqmRoot<?> root ) {
			final EntityPersister entityDescriptor = factory.getMappingMetamodel()
					.getEntityDescriptor( root.getEntityName() );
			return entityDescriptor.findAttributeMapping( propertyName );
		}
		else {
			// try to derive the subpart from the lhs
			final ValuedModelPart subPart = getSubPart( path.getLhs(), path.getNavigablePath().getLocalName() );
			if ( subPart instanceof EmbeddableValuedModelPart embeddable ) {
				return embeddable.getEmbeddableTypeDescriptor().findAttributeMapping( propertyName );
			}
			else if ( subPart instanceof EntityValuedModelPart entity ) {
				return entity.getEntityMappingType().findAttributeMapping( propertyName );
			}
			else if ( subPart instanceof PluralAttributeMapping plural ) {
				final CollectionPart.Nature nature = castNonNull( CollectionPart.Nature.fromNameExact( propertyName ) );
				return switch ( nature ) {
					case ELEMENT -> plural.getElementDescriptor();
					case ID -> plural.getIdentifierDescriptor();
					case INDEX -> plural.getIndexDescriptor();
				};
			}
		}
		return null;
	}
}
