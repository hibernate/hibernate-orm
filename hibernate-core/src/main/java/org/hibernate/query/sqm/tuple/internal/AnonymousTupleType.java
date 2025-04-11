/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tuple.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hibernate.Incubating;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.metamodel.mapping.internal.SqlTypedMappingImpl;
import org.hibernate.metamodel.model.domain.SimpleDomainType;
import org.hibernate.query.sqm.tree.from.SqmDomainType;
import org.hibernate.query.sqm.tuple.TupleType;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.SemanticException;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmPluralPersistentAttribute;
import org.hibernate.query.sqm.tree.select.SqmSelectClause;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.ObjectArrayJavaType;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.internal.util.collections.CollectionHelper.linkedMapOfSize;


/**
 * @author Christian Beikov
 */
@Incubating
public class AnonymousTupleType<T> implements TupleType<T>, SqmDomainType<T>, ReturnableType<T>, SqmPathSource<T> {

	private final JavaType<T> javaTypeDescriptor;
	private final @Nullable NavigablePath[] componentSourcePaths;
	private final SqmExpressible<?>[] expressibles;
	private final String[] componentNames;
	private final Map<String, Integer> componentIndexMap;

	public AnonymousTupleType(SqmSubQuery<T> subQuery) {
		this( extractSqmExpressibles( subQuery ) );
	}

	public AnonymousTupleType(SqmSelectableNode<?>[] components) {
		expressibles = new SqmExpressible<?>[components.length];
		componentSourcePaths = new NavigablePath[components.length];
		for ( int i = 0; i < components.length; i++ ) {
			expressibles[i] = components[i].getNodeType();
			if ( components[i] instanceof SqmPath<?> path ) {
				componentSourcePaths[i] = path.getNavigablePath();
			}
		}
		componentNames = new String[components.length];
		//noinspection unchecked
		javaTypeDescriptor = (JavaType<T>) new ObjectArrayJavaType( getTypeDescriptors( components ) );
		componentIndexMap = linkedMapOfSize( components.length );
		for ( int i = 0; i < components.length; i++ ) {
			final SqmSelectableNode<?> component = components[i];
			final String alias = component.getAlias();
			if ( alias == null ) {
				throw new SemanticException( "Select item at position " + (i+1) + " in select list has no alias"
						+ " (aliases are required in CTEs and in subqueries occurring in from clause)" );
			}
			componentIndexMap.put( alias, i );
			componentNames[i] = alias;
		}
	}

	public AnonymousTupleType(SqmExpressible<?>[] expressibles, String[] componentNames) {
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

	private static SqmSelectableNode<?>[] extractSqmExpressibles(SqmSubQuery<?> subQuery) {
		final SqmSelectClause selectClause = subQuery.getQuerySpec().getSelectClause();
		if ( selectClause == null || selectClause.getSelectionItems().isEmpty() ) {
			throw new IllegalArgumentException( "subquery has no selection items" );
		}
		// todo: right now, we "snapshot" the state of the subquery when creating this type, but maybe we shouldn't?
		//  i.e. what if the subquery changes later on? Or should we somehow mark the subquery to signal,
		//  that changes to the select clause are invalid after a certain point?
		return selectClause.getSelectionItems().toArray( SqmSelectableNode[]::new );
	}

	private static JavaType<?>[] getTypeDescriptors(SqmSelectableNode<?>[] components) {
		final JavaType<?>[] typeDescriptors = new JavaType<?>[components.length];
		for ( int i = 0; i < components.length; i++ ) {
			typeDescriptors[i] = components[i].getExpressible().getExpressibleJavaType();
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
			if ( expressionType instanceof SqlTypedMapping sqlTypedMapping ) {
				jdbcMappings[i] = sqlTypedMapping;
			}
			else {
				jdbcMappings[i] = new SqlTypedMappingImpl( expressionType.getSingleJdbcMapping() );
			}
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
	public SqmExpressible<?> get(int index) {
		return expressibles[index];
	}

	@Override
	public SqmExpressible<?> get(String componentName) {
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
	public SqmPathSource<?> findSubPathSource(String name) {
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
		else if ( sqmType instanceof BasicType<T> ) {
			return new AnonymousTupleSimpleSqmPathSource<>(
					name,
					sqmType,
					BindableType.SINGULAR_ATTRIBUTE
			);
		}
		else {
			return new AnonymousTupleSqmAssociationPathSourceNew<>(
					name,
					(SqmPathSource<T>) expressible,
					sqmType,
					(SimpleDomainType<T>) sqmType
			);
		}
	}

	@Override
	public JavaType<T> getExpressibleJavaType() {
		return javaTypeDescriptor;
	}

	@Override
	public BindableType getBindableType() {
		return BindableType.ENTITY_TYPE;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.ENTITY;
	}

	@Override
	public String getPathName() {
		return "tuple" + System.identityHashCode( this );
	}

	@Override
	public SqmDomainType<T> getSqmPathType() {
		return this;
	}

	@Override
	public SqmDomainType<T> getSqmType() {
		return this;
	}

	@Override
	public SqmPath<T> createSqmPath(SqmPath<?> lhs, SqmPathSource<?> intermediatePathSource) {
		throw new UnsupportedMappingException(
				"AnonymousTupleType cannot be used to create an SqmPath - that would be an SqmFrom which are created directly"
		);
	}

	@Override
	public Class<T> getBindableJavaType() {
		//noinspection unchecked
		return (Class<T>) javaTypeDescriptor.getJavaType();
	}

	@Override
	public Class<T> getJavaType() {
		return getBindableJavaType();
	}

	@Override
	public String toString() {
		return "AnonymousTupleType" + Arrays.toString( expressibles );
	}

}
