/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.derived;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import jakarta.persistence.metamodel.Bindable;
import org.hibernate.Incubating;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.SimpleDomainType;
import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.TupleType;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.SemanticException;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.select.SqmSelectClause;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.ObjectArrayJavaType;

import jakarta.persistence.metamodel.Attribute;


/**
 * @author Christian Beikov
 */
@Incubating
public class AnonymousTupleType<T> implements TupleType<T>, DomainType<T>, ReturnableType<T>, SqmPathSource<T> {

	private final ObjectArrayJavaType javaTypeDescriptor;
	private final SqmSelectableNode<?>[] components;
	private final Map<String, Integer> componentIndexMap;

	public AnonymousTupleType(SqmSubQuery<T> subQuery) {
		this( extractSqmExpressibles( subQuery ) );
	}

	public AnonymousTupleType(SqmSelectableNode<?>[] components) {
		this.components = components;
		this.javaTypeDescriptor = new ObjectArrayJavaType( getTypeDescriptors( components ) );
		final Map<String, Integer> map = CollectionHelper.linkedMapOfSize( components.length );
		for ( int i = 0; i < components.length; i++ ) {
			final SqmSelectableNode<?> component = components[i];
			final String alias = component.getAlias();
			if ( alias == null ) {
				throw new SemanticException( "Select item at position " + (i+1) + " in select list has no alias"
						+ " (aliases are required in CTEs and in subqueries occurring in from clause)" );
			}
			map.put( alias, i );
		}
		this.componentIndexMap = map;
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

	public AnonymousTupleTableGroupProducer resolveTableGroupProducer(
			String aliasStem,
			List<SqlSelection> sqlSelections,
			FromClauseAccess fromClauseAccess) {
		return new AnonymousTupleTableGroupProducer( this, aliasStem, sqlSelections, fromClauseAccess );
	}

	public List<String> determineColumnNames() {
		final int componentCount = componentCount();
		final List<String> columnNames = new ArrayList<>( componentCount );
		for ( int i = 0; i < componentCount; i++ ) {
			final SqmSelectableNode<?> selectableNode = getSelectableNode( i );
			final String componentName = getComponentName( i );
			if ( selectableNode instanceof SqmPath<?> ) {
				addColumnNames(
						columnNames,
						( (SqmPath<?>) selectableNode ).getNodeType().getSqmPathType(),
						componentName
				);
			}
			else {
				columnNames.add( componentName );
			}
		}
		return columnNames;
	}

	private static void addColumnNames(List<String> columnNames, DomainType<?> domainType, String componentName) {
		if ( domainType instanceof EntityDomainType<?> ) {
			final EntityDomainType<?> entityDomainType = (EntityDomainType<?>) domainType;
			final SingularPersistentAttribute<?, ?> idAttribute = entityDomainType.findIdAttribute();
			final String idPath = idAttribute == null ? componentName : componentName + "_" + idAttribute.getName();
			addColumnNames( columnNames, entityDomainType.getIdentifierDescriptor().getSqmPathType(), idPath );
		}
		else if ( domainType instanceof ManagedDomainType<?> ) {
			for ( Attribute<?, ?> attribute : ( (ManagedDomainType<?>) domainType ).getAttributes() ) {
				if ( !( attribute instanceof SingularPersistentAttribute<?, ?> ) ) {
					throw new IllegalArgumentException( "Only embeddables without collections are supported" );
				}
				final DomainType<?> attributeType = ( (SingularPersistentAttribute<?, ?>) attribute ).getType();
				addColumnNames( columnNames, attributeType, componentName + "_" + attribute.getName() );
			}
		}
		else {
			columnNames.add( componentName );
		}
	}

	@Override
	public int componentCount() {
		return components.length;
	}

	@Override
	public String getComponentName(int index) {
		return components[index].getAlias();
	}

	@Override
	public List<String> getComponentNames() {
		return new ArrayList<>( componentIndexMap.keySet() );
	}

	@Override
	public SqmExpressible<?> get(int index) {
		return components[index].getExpressible();
	}

	@Override
	public SqmExpressible<?> get(String componentName) {
		final Integer index = componentIndexMap.get( componentName );
		return index == null ? null : components[index].getExpressible();
	}

	protected Integer getIndex(String componentName) {
		return componentIndexMap.get( componentName );
	}

	public SqmSelectableNode<?> getSelectableNode(int index) {
		return components[index];
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name) {
		final Integer index = componentIndexMap.get( name );
		if ( index == null ) {
			return null;
		}
		final SqmSelectableNode<?> component = components[index];
		if ( component instanceof SqmPath<?> ) {
			final SqmPath<?> sqmPath = (SqmPath<?>) component;
			final Bindable<?> model = sqmPath.getModel();
			if ( model instanceof SingularPersistentAttribute<?, ?> ) {
				//noinspection unchecked,rawtypes
				return new AnonymousTupleSqmAssociationPathSource(
						name,
						sqmPath,
						( (SingularPersistentAttribute<?, ?>) model ).getType()
				);
			}
			else if ( model instanceof PluralPersistentAttribute<?, ?, ?> ) {
				//noinspection unchecked,rawtypes
				return new AnonymousTupleSqmAssociationPathSource(
						name,
						sqmPath,
						( (PluralPersistentAttribute<?, ?, ?>) model ).getElementType()
				);
			}
			else if ( sqmPath.getNodeType() instanceof EntityDomainType<?> ) {
				//noinspection unchecked,rawtypes
				return new AnonymousTupleSqmAssociationPathSource(
						name,
						sqmPath,
						(SimpleDomainType<?>) sqmPath.getNodeType()
				);
			}
			else {
				return new AnonymousTupleSqmPathSource<>( name, sqmPath );
			}
		}
		else {
			return new AnonymousTupleSimpleSqmPathSource<>(
					name,
					component.getExpressible().getSqmType(),
					BindableType.SINGULAR_ATTRIBUTE
			);
		}
	}

	@Override
	public JavaType<T> getExpressibleJavaType() {
		//noinspection unchecked
		return (JavaType<T>) javaTypeDescriptor;
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
	public DomainType<T> getSqmPathType() {
		return this;
	}

	@Override
	public DomainType<T> getSqmType() {
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
		return "AnonymousTupleType" + Arrays.toString( components );
	}

}
