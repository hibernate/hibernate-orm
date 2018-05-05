/*  
 * Hibernate, Relational Persistence for Idiomatic Java  
 *  
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.  
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.  
 */
package org.hibernate.criterion;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.TransactionRequiredException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Fetch;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;

enum JoinType {

	FETCH, JOIN;
}

enum Operator {
	EQUAL("EQUAL"), NOT_EQUAL("NOT_EQUAL"), CONTAINS("CONTAINS"), NOT_CONTAINS("NOT_CONTAINS"), IN("IN"), NOT_IN(
			"NOT_IN"), BEGINS_WITH("BEGINS_WITH"), ENDS_WITH("ENDS_WITH");

	private String value;

	private Operator(String value) {
		this.value = value;
	}

	public static Operator fromValue(String value) {
		for ( Operator operaor : values() ) {
			if ( operaor.value.equalsIgnoreCase( value ) ) {
				return operaor;
			}
		}
		throw new IllegalArgumentException(
				"Unknown enum type " + value + ", Allowed values are " + Arrays.toString( values() ) );
	}
}

class Exp {

	private SingularAttribute<?, ?> propertyName;

	private Operator operator;

	private Object valueObject;

	public SingularAttribute<?, ?> getPropertyName() {
		return propertyName;
	}

	public void setPropertyName(SingularAttribute<?, ?> propertyName) {
		this.propertyName = propertyName;
	}

	public Operator getOperator() {
		return operator;
	}

	public void setOperator(Operator operator) {
		this.operator = operator;
	}

	public Object getValueObject() {
		return valueObject;
	}

	public void setValueObject(Object valueObject) {
		this.valueObject = valueObject;
	}

}

class FilterNode {

	private EntityManager em;

	private Class<?> entityClass;

	private ArrayList<Exp> expressions = new ArrayList<Exp>();

	private ArrayList<SetAttribute<?, ?>> childFields = new ArrayList<SetAttribute<?, ?>>();

	private ArrayList<SingularAttribute<?, ?>> siblingFields = new ArrayList<SingularAttribute<?, ?>>();

	private HashMap<Object, FilterNode> childNodeMap = new HashMap<Object, FilterNode>();

	public FilterNode(Class<?> className, EntityManager em) {
		this.entityClass = className;
		this.em = em;
	}

	public ArrayList<SetAttribute<?, ?>> getChildFields() {
		return childFields;
	}

	public void setChildFields(ArrayList<SetAttribute<?, ?>> childFields) {
		this.childFields = childFields;
	}

	public Class<?> getEntityClass() {
		return entityClass;
	}

	public void setEntityClass(Class<?> entityClass) {
		this.entityClass = entityClass;
	}

	public ArrayList<Exp> getExps() {
		return expressions;
	}

	public void setExps(ArrayList<Exp> expressions) {
		this.expressions = expressions;
	}

	public void addCondition(String propertyName, Operator operator, Object valueObj) {
		Exp e = new Exp();
		EntityType<?> entity = em.getMetamodel().entity( this.entityClass );
		e.setPropertyName( entity.getSingularAttribute( propertyName ) );
		e.setOperator( operator );
		e.setValueObject( valueObj );
		getExps().add( e );

	}

	public void addChild(SetAttribute<?, ?> child, FilterNode clientAccountRelationshipNode) {
		getChildFields().add( child );
		getChildNodeMap().put( child, clientAccountRelationshipNode );
	}

	public ArrayList<SingularAttribute<?, ?>> getSiblingFields() {
		return siblingFields;
	}

	public void setSiblingFields(ArrayList<SingularAttribute<?, ?>> siblingFields) {
		this.siblingFields = siblingFields;
	}

	public void addSibling(SingularAttribute<?, ?> singularAttribute, FilterNode clientEmploymentRelationship) {
		getSiblingFields().add( singularAttribute );
		getChildNodeMap().put( singularAttribute, clientEmploymentRelationship );
	}

	public HashMap<Object, FilterNode> getChildNodeMap() {
		return childNodeMap;
	}

	public void setChildNodeMap(HashMap<Object, FilterNode> childNodeMap) {
		this.childNodeMap = childNodeMap;
	}
}

/**
 * @author
 * <h1>Vipin Chandran Palangate</h1>
 * <p>
 * This class is written for generating the CriteriaQuery from given Entity object with example values. Similar to
 * org.hibernate.criterion.Example , expect ExampleWithAssociation will consider association as well
 * <p>
 */
public class ExampleWithAssociation {

	static Set<Class<?>> datatypes = null;
	static {
		datatypes = new HashSet<Class<?>>();
		datatypes.add( String.class );
		datatypes.add( Long.class );
		datatypes.add( Date.class );
		datatypes.add( Double.class );
		datatypes.add( Short.class );
		datatypes.add( Integer.class );
		datatypes.add( Float.class );
	}

	private static final String serialVersionUID = "serialVersionUID";

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public CriteriaQuery getCriteriaQuery(FilterNode filterNode, EntityManager em) throws Exception {
		ArrayList<Predicate> predicateList = new ArrayList<>();
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<?> criteriaQuery = builder.createQuery( filterNode.getEntityClass() );
		criteriaQuery.distinct( true );
		try {
			Root pRoot = criteriaQuery.from( filterNode.getEntityClass() );
			criteriaQuery.select( pRoot );
			filter( criteriaQuery, builder, pRoot, null, filterNode, null, predicateList );
			if ( !predicateList.isEmpty() ) {
				Predicate[] predArray = new Predicate[predicateList.size()];
				predicateList.toArray( predArray );
				criteriaQuery.where( predArray );
			}
		}
		catch (Exception e) {
			throw new Exception( e.getMessage() );
		}
		return criteriaQuery;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Fetch<?, ?> filter(CriteriaQuery<?> criteriaQuery, CriteriaBuilder builder, Root parent, Fetch child,
			FilterNode parentNode, FilterNode childNode, List predicateList) throws Exception {
		try {
			Fetch<?, ?> tempChild = null;
			FilterNode currentNode = null;
			if ( childNode == null && parentNode != null ) {
				currentNode = parentNode;
			}
			else {
				currentNode = childNode;
			}
			for ( Exp expression : currentNode.getExps() ) {
				SingularAttribute<?, ?> propertyName = expression.getPropertyName();
				Operator operator = expression.getOperator();
				Object valueObject = expression.getValueObject();
				Predicate equal = null;
				switch ( operator ) {
					case EQUAL:
						if ( child == null ) {
							if ( valueObject instanceof Long ) {
								equal = builder.equal( parent.get( propertyName ), valueObject );
							}
							else if ( valueObject instanceof String ) {
								equal = builder.equal( parent.get( propertyName ), valueObject );
							}
							else if ( valueObject instanceof Short ) {
								equal = builder.equal( parent.get( propertyName ), valueObject );
							}
							else if ( valueObject instanceof Object ) {
								Field[] declaredFields = valueObject.getClass().getDeclaredFields();
								for ( Field f : declaredFields ) {
									f.setAccessible( true );
									Object value = f.get( valueObject );
									if ( value != null ) {
										equal = builder.equal( parent.get( propertyName ).get( f.getName() ), value );
									}
								}
							}
						}
						else {
							equal = builder.equal( ( (Join) child ).get( propertyName ), valueObject );
						}
						predicateList.add( equal );
						break;
					case NOT_EQUAL:
					case CONTAINS:
					case NOT_CONTAINS:
					case IN:
					case NOT_IN:
					case BEGINS_WITH:
					case ENDS_WITH:
					default:
				}
			}
			for ( SetAttribute<?, ?> setAttribute : currentNode.getChildFields() ) {
				if ( setAttribute == null ) {
					continue;
				}
				if ( child == null ) {
					child = parent.fetch( setAttribute );
				}
				else if ( parent == null ) {
					tempChild = child.fetch( setAttribute );
				}
				else if ( child != null && parent != null ) {
					child = parent.fetch( setAttribute );
				}
				FilterNode node = currentNode.getChildNodeMap().get( setAttribute );
				if ( tempChild != null ) {
					filter( criteriaQuery, builder, null, tempChild, currentNode, node, predicateList );
				}
				else {
					filter( criteriaQuery, builder, null, child, currentNode, node, predicateList );
				}
			}
			for ( SingularAttribute<?, ?> singularAttribute : currentNode.getSiblingFields() ) {
				if ( singularAttribute == null ) {
					continue;
				}
				if ( child == null ) {
					child = parent.fetch( singularAttribute );
				}
				else if ( parent == null ) {
					tempChild = child.fetch( singularAttribute );
				}
				else if ( child != null && parent != null ) {
					child = parent.fetch( singularAttribute );
				}
				FilterNode node = currentNode.getChildNodeMap().get( singularAttribute );
				if ( tempChild != null ) {
					filter( criteriaQuery, builder, null, tempChild, currentNode, node, predicateList );
				}
				else {
					filter( criteriaQuery, builder, null, child, currentNode, node, predicateList );
				}
			}
			return child;
		}
		catch (EntityExistsException ee) {
			throw new EntityExistsException( ee.getMessage() );
		}
		catch (IllegalArgumentException iae) {
			throw new IllegalArgumentException( iae.getMessage() );
		}
		catch (TransactionRequiredException tre) {
			throw new TransactionRequiredException( tre.getMessage() );
		}
		catch (Exception e) {
			throw new Exception( e.getMessage() );
		}
	}

	public FilterNode getFilterNodeFromEntity(Object entityObject, Object parentEntityObject, FilterNode filterNode,
			EntityManager em) throws Exception {
		try {
			if ( filterNode == null ) {
				filterNode = new FilterNode( entityObject.getClass(), em );
			}
			Metamodel metamodel = (Metamodel) em.getMetamodel();
			EntityType<?> entity = metamodel.entity( entityObject.getClass() );
			Field[] declaredFields = entityObject.getClass().getDeclaredFields();
			for ( Field f : declaredFields ) {
				f.setAccessible( true );
				if ( f.get( entityObject ) == null
						|| ( f.get( entityObject ) != null && f.get( entityObject ).equals( parentEntityObject ) )
						|| f.getName().equalsIgnoreCase( serialVersionUID ) ) {
					continue;
				}
				else if ( datatypes.contains( f.getType() ) ) {
					filterNode.addCondition( f.getName(), Operator.EQUAL, f.get( entityObject ) );
				}
				else if ( f.get( entityObject ) instanceof Collection<?> ) {
					Collection<?> collectionObject = (Collection<?>) f.get( entityObject );
					Iterator<?> iterator = collectionObject.iterator();
					while ( iterator.hasNext() ) {
						Object obj = iterator.next();
						if ( obj != null ) {
							FilterNode childNode = new FilterNode( obj.getClass(), em );
							filterNode.addChild( (SetAttribute<?, ?>) entity.getDeclaredSet( f.getName() ), childNode );
							getFilterNodeFromEntity( obj, entityObject, childNode, em );
						}
					}
				}
				else if ( f.get( entityObject ) instanceof Object ) {
					FilterNode siblingNode = new FilterNode( f.get( entityObject ).getClass(), em );
					filterNode.addSibling( (SingularAttribute<?, ?>) entity.getSingularAttribute( f.getName() ),
							siblingNode );
					getFilterNodeFromEntity( f.get( entityObject ), entityObject, siblingNode, em );
				}
			}
		}
		catch (Exception e) {
			throw new Exception( "Exception while creating Filternode from the entity object" );
		}
		return filterNode;
	}

	public CriteriaQuery<?> getCriteriaQuery(Object entityObject, EntityManager em) throws Exception {
		try {
			FilterNode filterNode = getFilterNodeFromEntity( entityObject, null, null, em );
			return getCriteriaQuery( filterNode, em );
		}
		catch (Exception e) {
			throw new Exception( "Exception while getting entity object" );
		}
	}
}
