/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tuple.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.Incubating;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.metamodel.mapping.internal.SqlTypedMappingImpl;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.metamodel.model.domain.SimpleDomainType;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.domain.SqmDomainType;
import org.hibernate.query.sqm.tree.domain.SqmPluralPersistentAttribute;
import org.hibernate.query.sqm.tree.select.SqmSelectQuery;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.query.sqm.tuple.TupleType;
import org.hibernate.query.SemanticException;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.select.SqmSelectClause;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.ObjectArrayJavaType;

import org.checkerframework.checker.nullness.qual.Nullable;

import static jakarta.persistence.metamodel.Bindable.BindableType.ENTITY_TYPE;
import static jakarta.persistence.metamodel.Type.PersistenceType.ENTITY;
import static org.hibernate.internal.util.collections.CollectionHelper.linkedMapOfSize;


/**
 * @author Christian Beikov
 */
@Incubating
public class AnonymousTupleType<T>
		implements TupleType<T>, SqmDomainType<T>, SqmPathSource<T> {

	private final JavaType<T> javaTypeDescriptor;
	private final @Nullable NavigablePath[] componentSourcePaths;
	private final SqmBindableType<?>[] expressibles;
	private final String[] componentNames;
	private final Map<String, Integer> componentIndexMap;

	public AnonymousTupleType(SqmSelectQuery<T> selectQuery) {
		final SqmSelectClause selectClause = selectQuery.getQueryPart()
				.getFirstQuerySpec()
				.getSelectClause();

		if ( selectClause.getSelections().isEmpty() ) {
			throw new IllegalArgumentException( "selectQuery has no selection items" );
		}
		// todo: right now, we "snapshot" the state of the selectQuery when creating this type, but maybe we shouldn't?
		//  i.e. what if the selectQuery changes later on? Or should we somehow mark the selectQuery to signal,
		//  that changes to the select clause are invalid after a certain point?

		final List<SqmSelection<?>> selections = selectClause.getSelections();
		final List<SqmSelectableNode<?>> selectableNodes = new ArrayList<>();
		final List<String> aliases = new ArrayList<>();
		for ( SqmSelection<?> selection : selections ) {
			final boolean compound = selection.getSelectableNode().isCompoundSelection();
			selection.getSelectableNode().visitSubSelectableNodes( node -> {
				selectableNodes.add( node );
				if ( compound ) {
					aliases.add( node.getAlias() );
				}
			} );
			if ( !compound ) {
				// for compound selections we use the sub-selectable nodes aliases
				aliases.add( selection.getAlias() );
			}
		}

		expressibles = new SqmBindableType<?>[selectableNodes.size()];
		componentSourcePaths = new NavigablePath[selectableNodes.size()];
		componentNames = new String[selectableNodes.size()];
		//noinspection unchecked
		javaTypeDescriptor = (JavaType<T>) new ObjectArrayJavaType( getTypeDescriptors( selectableNodes ) );
		componentIndexMap = linkedMapOfSize( selectableNodes.size() );
		for ( int i = 0; i < selectableNodes.size(); i++ ) {
			expressibles[i] = selectableNodes.get( i ).getNodeType();
			if ( selectableNodes.get( i ) instanceof SqmPath<?> path ) {
				componentSourcePaths[i] = path.getNavigablePath();
			}
			String alias = aliases.get( i );
			if ( alias == null ) {
				throw new SemanticException( "Select item at position " + (i+1) + " in select list has no alias"
						+ " (aliases are required in CTEs and in subqueries occurring in from clause)" );
			}
			componentIndexMap.put( alias, i );
			componentNames[i] = alias;
		}
	}

	public AnonymousTupleType(SqmBindableType<?>[] expressibles, String[] componentNames) {
		this.expressibles = expressibles;
		this.componentNames = componentNames;

		componentSourcePaths = new NavigablePath[componentNames.length];
		componentIndexMap = linkedMapOfSize( expressibles.length );
		int elementIndex = -1;
		for ( int i = 0; i < componentNames.length; i++ ) {
			if ( CollectionPart.Nature.ELEMENT.getName().equals( componentNames[i] ) ) {
				elementIndex = i;
			}
			componentIndexMap.put( componentNames[i], i );
		}
		// The expressible java type of this tuple type must be equal to the element type if it exists
		//noinspection unchecked
		javaTypeDescriptor =
				elementIndex == -1
						? (JavaType<T>) new ObjectArrayJavaType( getTypeDescriptors( expressibles ) )
						: (JavaType<T>) expressibles[elementIndex].getExpressibleJavaType();
	}

	@Override
	public String getTypeName() {
		return SqmDomainType.super.getTypeName();
	}

	private static JavaType<?>[] getTypeDescriptors(List<SqmSelectableNode<?>> components) {
		final JavaType<?>[] typeDescriptors = new JavaType<?>[components.size()];
		for ( int i = 0; i < components.size(); i++ ) {
			typeDescriptors[i] = components.get( i ).getExpressible().getExpressibleJavaType();
		}
		return typeDescriptors;
	}

	private static JavaType<?>[] getTypeDescriptors(SqmExpressible<?>[] components) {
		final JavaType<?>[] typeDescriptors = new JavaType<?>[components.length];
		for ( int i = 0; i < components.length; i++ ) {
			typeDescriptors[i] = components[i].getExpressibleJavaType();
		}
		return typeDescriptors;
	}

	public static SqlTypedMapping[] toSqlTypedMappings(List<SqlSelection> sqlSelections) {
		final SqlTypedMapping[] jdbcMappings = new SqlTypedMapping[sqlSelections.size()];
		for ( int i = 0; i < sqlSelections.size(); i++ ) {
			final JdbcMappingContainer expressionType = sqlSelections.get( i ).getExpressionType();
			jdbcMappings[i] =
					expressionType instanceof SqlTypedMapping sqlTypedMapping
							? sqlTypedMapping
							: new SqlTypedMappingImpl( expressionType.getSingleJdbcMapping() );
		}
		return jdbcMappings;
	}
	public AnonymousTupleTableGroupProducer resolveTableGroupProducer(
			String aliasStem,
			List<SqlSelection> sqlSelections,
			FromClauseAccess fromClauseAccess) {
		return resolveTableGroupProducer( aliasStem, toSqlTypedMappings( sqlSelections ), fromClauseAccess );
	}

	public AnonymousTupleTableGroupProducer resolveTableGroupProducer(
			String aliasStem,
			SqlTypedMapping[] jdbcMappings,
			FromClauseAccess fromClauseAccess) {
		return new AnonymousTupleTableGroupProducer( this, aliasStem, jdbcMappings, fromClauseAccess );
	}

	@Override
	public int componentCount() {
		return expressibles.length;
	}

	@Override
	public String getComponentName(int index) {
		return componentNames[index];
	}

	@Override
	public List<String> getComponentNames() {
		return new ArrayList<>( componentIndexMap.keySet() );
	}

	@Override
	public SqmBindableType<?> get(int index) {
		return expressibles[index];
	}

	@Override
	public @Nullable SqmBindableType<?> get(String componentName) {
		final Integer index = componentIndexMap.get( componentName );
		return index == null ? null : expressibles[index];
	}

	protected Integer getIndex(String componentName) {
		return componentIndexMap.get( componentName );
	}

	public @Nullable NavigablePath getComponentSourcePath(int index) {
		return componentSourcePaths[index];
	}

	@Override
	public @Nullable SqmPathSource<?> findSubPathSource(String name) {
		final Integer index = componentIndexMap.get( name );
		return index == null ? null : subpathSource( name, expressibles[index] );
	}

	private static <T> SqmPathSource<T> subpathSource(String name, SqmExpressible<T> expressible) {
		final SqmDomainType<T> sqmType = expressible.getSqmType();
		if ( expressible instanceof SqmPluralPersistentAttribute<?, ?, T> pluralAttribute ) {
			return new AnonymousTupleSqmAssociationPathSourceNew<>(
					name,
					pluralAttribute,
					sqmType,
					pluralAttribute.getElementType()
			);
		}
		else if ( sqmType instanceof BasicDomainType<?> ) {
			return new AnonymousTupleSimpleSqmPathSource<>(
					name,
					sqmType,
					BindableType.SINGULAR_ATTRIBUTE
			);
		}
		// TODO: introduce SqmSimpleDomainType to get rid of unchecked cast
		else if ( sqmType instanceof SimpleDomainType<?> ) {
			return new AnonymousTupleSqmAssociationPathSourceNew<>(
					name,
					(SqmPathSource<T>) expressible,
					sqmType,
					(SimpleDomainType<T>) sqmType
			);
		}
		else {
			throw new AssertionFailure( "Unsupported domain type " + sqmType );
		}
	}

	@Override
	public JavaType<T> getExpressibleJavaType() {
		return javaTypeDescriptor;
	}

	@Override
	public BindableType getBindableType() {
		// TODO: should this be SINGULAR_ATTRIBUTE
		return ENTITY_TYPE;
	}

	@Override
	public PersistenceType getPersistenceType() {
		// TODO: should this be EMBEDDABLE
		return ENTITY;
	}

	@Override
	public String getPathName() {
		return "tuple" + System.identityHashCode( this );
	}

	@Override
	public SqmDomainType<T> getPathType() {
		return this;
	}

	@Override
	public @Nullable SqmDomainType<T> getSqmType() {
		return this;
	}

	@Override
	public SqmPath<T> createSqmPath(SqmPath<?> lhs, @Nullable SqmPathSource<?> intermediatePathSource) {
		throw new UnsupportedMappingException(
				"AnonymousTupleType cannot be used to create an SqmPath - that would be an SqmFrom which are created directly"
		);
	}

	@Override
	public Class<T> getBindableJavaType() {
		return javaTypeDescriptor.getJavaTypeClass();
	}

	@Override
	public Class<T> getJavaType() {
		return javaTypeDescriptor.getJavaTypeClass();
	}

	@Override
	public String toString() {
		return "AnonymousTupleType" + Arrays.toString( expressibles );
	}

}
