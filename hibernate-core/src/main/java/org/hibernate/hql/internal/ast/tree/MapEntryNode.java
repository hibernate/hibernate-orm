/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.NameGenerator;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.AliasGenerator;
import org.hibernate.sql.SelectExpression;
import org.hibernate.sql.SelectFragment;
import org.hibernate.transform.BasicTransformerAdapter;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

import antlr.SemanticException;

/**
 * Tree node representing reference to the entry ({@link Map.Entry}) of a Map association.
 *
 * @author Steve Ebersole
 */
public class MapEntryNode extends AbstractMapComponentNode implements AggregatedSelectExpression {
	private static class LocalAliasGenerator implements AliasGenerator {
		private final int base;
		private int counter;

		private LocalAliasGenerator(int base) {
			this.base = base;
		}

		@Override
		public String generateAlias(String sqlExpression) {
			return NameGenerator.scalarName( base, counter++ );
		}
	}

	private int scalarColumnIndex = -1;

	@Override
	protected String expressionDescription() {
		return "entry(*)";
	}

	@Override
	public Class getAggregationResultType() {
		return Map.Entry.class;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Type resolveType(QueryableCollection collectionPersister) {
		final Type keyType = collectionPersister.getIndexType();
		final Type valueType = collectionPersister.getElementType();
		types.add( keyType );
		types.add( valueType );
		mapEntryBuilder = new MapEntryBuilder();

		// an entry (as an aggregated select expression) does not have a type...
		return null;
	}

	@Override
	protected String[] resolveColumns(QueryableCollection collectionPersister) {
		List selections = new ArrayList();
		determineKeySelectExpressions( collectionPersister, selections );
		determineValueSelectExpressions( collectionPersister, selections );

		String text = "";
		String[] columns = new String[selections.size()];
		for ( int i = 0; i < selections.size(); i++ ) {
			SelectExpression selectExpression = (SelectExpression) selections.get( i );
			text += ( ", " + selectExpression.getExpression() + " as " + selectExpression.getAlias() );
			columns[i] = selectExpression.getExpression();
		}

		text = text.substring( 2 ); //strip leading ", "
		setText( text );
		setResolved();
		return columns;
	}

	private void determineKeySelectExpressions(QueryableCollection collectionPersister, List selections) {
		AliasGenerator aliasGenerator = new LocalAliasGenerator( 0 );
		appendSelectExpressions( collectionPersister.getIndexColumnNames(), selections, aliasGenerator );
		Type keyType = collectionPersister.getIndexType();
		if ( keyType.isEntityType() ) {
			MapKeyEntityFromElement mapKeyEntityFromElement = findOrAddMapKeyEntityFromElement( collectionPersister );
			Queryable keyEntityPersister = mapKeyEntityFromElement.getQueryable();
			SelectFragment fragment = keyEntityPersister.propertySelectFragmentFragment(
					mapKeyEntityFromElement.getTableAlias(),
					null,
					false
			);
			appendSelectExpressions( fragment, selections, aliasGenerator );
		}
	}

	@SuppressWarnings({"unchecked", "ForLoopReplaceableByForEach"})
	private void appendSelectExpressions(String[] columnNames, List selections, AliasGenerator aliasGenerator) {
		for ( int i = 0; i < columnNames.length; i++ ) {
			selections.add(
					new BasicSelectExpression(
							collectionTableAlias() + '.' + columnNames[i],
							aliasGenerator.generateAlias( columnNames[i] )
					)
			);
		}
	}

	@SuppressWarnings({"unchecked", "WhileLoopReplaceableByForEach"})
	private void appendSelectExpressions(SelectFragment fragment, List selections, AliasGenerator aliasGenerator) {
		Iterator itr = fragment.getColumns().iterator();
		while ( itr.hasNext() ) {
			final String column = (String) itr.next();
			selections.add(
					new BasicSelectExpression( column, aliasGenerator.generateAlias( column ) )
			);
		}
	}

	private void determineValueSelectExpressions(QueryableCollection collectionPersister, List selections) {
		AliasGenerator aliasGenerator = new LocalAliasGenerator( 1 );
		appendSelectExpressions( collectionPersister.getElementColumnNames(), selections, aliasGenerator );
		Type valueType = collectionPersister.getElementType();
		if ( valueType.isAssociationType() ) {
			EntityType valueEntityType = (EntityType) valueType;
			Queryable valueEntityPersister = (Queryable) sfi().getEntityPersister(
					valueEntityType.getAssociatedEntityName( sfi() )
			);
			SelectFragment fragment = valueEntityPersister.propertySelectFragmentFragment(
					elementTableAlias(),
					null,
					false
			);
			appendSelectExpressions( fragment, selections, aliasGenerator );
		}
	}

	private String collectionTableAlias() {
		return getFromElement().getCollectionTableAlias() != null
				? getFromElement().getCollectionTableAlias()
				: getFromElement().getTableAlias();
	}

	private String elementTableAlias() {
		return getFromElement().getTableAlias();
	}

	private static class BasicSelectExpression implements SelectExpression {
		private final String expression;
		private final String alias;

		private BasicSelectExpression(String expression, String alias) {
			this.expression = expression;
			this.alias = alias;
		}

		@Override
		public String getExpression() {
			return expression;
		}

		@Override
		public String getAlias() {
			return alias;
		}
	}

	public SessionFactoryImplementor sfi() {
		return getSessionFactoryHelper().getFactory();
	}

	@Override
	public void setText(String s) {
		if ( isResolved() ) {
			return;
		}
		super.setText( s );
	}

	@Override
	public void setScalarColumn(int i) throws SemanticException {
		this.scalarColumnIndex = i;
	}

	@Override
	public int getScalarColumnIndex() {
		return scalarColumnIndex;
	}

	@Override
	public void setScalarColumnText(int i) throws SemanticException {
	}

	@Override
	public boolean isScalar() {
		// Constructors are always considered scalar results.
		return true;
	}

	private List types = new ArrayList( 4 ); // size=4 to prevent resizing

	@Override
	public List getAggregatedSelectionTypeList() {
		return types;
	}

	private static final String[] ALIASES = {null, null};

	@Override
	public String[] getAggregatedAliases() {
		return ALIASES;
	}

	private MapEntryBuilder mapEntryBuilder;

	@Override
	public ResultTransformer getResultTransformer() {
		return mapEntryBuilder;
	}

	private static class MapEntryBuilder extends BasicTransformerAdapter {
		@Override
		public Object transformTuple(Object[] tuple, String[] aliases) {
			if ( tuple.length != 2 ) {
				throw new HibernateException( "Expecting exactly 2 tuples to transform into Map.Entry" );
			}
			return new EntryAdapter( tuple[0], tuple[1] );
		}
	}

	private static class EntryAdapter implements Map.Entry {
		private final Object key;
		private Object value;

		private EntryAdapter(Object key, Object value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public Object getValue() {
			return value;
		}

		@Override
		public Object getKey() {
			return key;
		}

		@Override
		public Object setValue(Object value) {
			Object old = this.value;
			this.value = value;
			return old;
		}

		@Override
		public boolean equals(Object o) {
			// IMPL NOTE : nulls are considered equal for keys and values according to Map.Entry contract
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			EntryAdapter that = (EntryAdapter) o;

			// make sure we have the same types...
			return ( key == null ? that.key == null : key.equals( that.key ) )
					&& ( value == null ? that.value == null : value.equals( that.value ) );

		}

		@Override
		public int hashCode() {
			int keyHash = key == null ? 0 : key.hashCode();
			int valueHash = value == null ? 0 : value.hashCode();
			return keyHash ^ valueHash;
		}
	}
}
