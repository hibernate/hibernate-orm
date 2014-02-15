/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.persister.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cfg.Settings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.internal.StaticFilterAliasGenerator;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.collections.JoinedIterator;
import org.hibernate.internal.util.collections.SingletonIterator;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.sql.SelectFragment;
import org.hibernate.sql.SimpleSelect;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
 * Implementation of the "table-per-concrete-class" or "roll-down" mapping 
 * strategy for an entity and its inheritence hierarchy.
 *
 * @author Gavin King
 */
public class UnionSubclassEntityPersister extends AbstractEntityPersister {

	// the class hierarchy structure
	private final String subquery;
	private final String tableName;
	//private final String rootTableName;
	private final String[] subclassClosure;
	private final String[] spaces;
	private final String[] subclassSpaces;
	private final Object discriminatorValue;
	private final String discriminatorSQLValue;
	private final Map subclassByDiscriminatorValue = new HashMap();

	private final String[] constraintOrderedTableNames;
	private final String[][] constraintOrderedKeyColumnNames;

	//INITIALIZATION:

	public UnionSubclassEntityPersister(
			final EntityBinding entityBinding,
			final EntityRegionAccessStrategy cacheAccessStrategy,
			final NaturalIdRegionAccessStrategy naturalIdRegionAccessStrategy,
			final SessionFactoryImplementor factory,
			final Mapping mapping) throws HibernateException {
		super(entityBinding, cacheAccessStrategy, naturalIdRegionAccessStrategy, factory );
		if ( getIdentifierGenerator() instanceof IdentityGenerator ) {
			throw new MappingException(
					"Cannot use identity column key generation with <union-subclass> mapping for: " +
							getEntityName()
			);
		}
		tableName = entityBinding.getPrimaryTable().getQualifiedName( factory.getDialect() );


		//Custom SQL
		// Custom sql
		customSQLInsert = new String[1];
		customSQLUpdate = new String[1];
		customSQLDelete = new String[1];
		insertCallable = new boolean[1];
		updateCallable = new boolean[1];
		deleteCallable = new boolean[1];
		insertResultCheckStyles = new ExecuteUpdateResultCheckStyle[1];
		updateResultCheckStyles = new ExecuteUpdateResultCheckStyle[1];
		deleteResultCheckStyles = new ExecuteUpdateResultCheckStyle[1];

		initializeCustomSql( entityBinding.getCustomInsert(), 0, customSQLInsert, insertCallable, insertResultCheckStyles );
		initializeCustomSql( entityBinding.getCustomUpdate(), 0, customSQLUpdate, updateCallable, updateResultCheckStyles );
		initializeCustomSql( entityBinding.getCustomDelete(), 0, customSQLDelete, deleteCallable, deleteResultCheckStyles );
		//discriminator
		{
			discriminatorValue = entityBinding.getSubEntityBindingId();
			discriminatorSQLValue = String.valueOf( discriminatorValue );
		}

		// PROPERTIES

		int subclassSpan = entityBinding.getSubEntityBindingClosureSpan() + 1;
		subclassClosure = new String[subclassSpan];
		subclassClosure[0] = getEntityName();

		// SUBCLASSES
		subclassByDiscriminatorValue.put(
				entityBinding.getSubEntityBindingId(),
				entityBinding.getEntityName()
		);
		if ( entityBinding.isPolymorphic() ) {
			int k=1;
			for(EntityBinding subEntityBinding : entityBinding.getPreOrderSubEntityBindingClosure()){
				subclassClosure[k++] = subEntityBinding.getEntityName();
				subclassByDiscriminatorValue.put( subEntityBinding.getSubEntityBindingId(), subEntityBinding.getEntityName() );
			}
		}


		//SPACES
		//TODO: i'm not sure, but perhaps we should exclude
		//      abstract denormalized tables?

		String[] synchronizedTableNames = entityBinding.getSynchronizedTableNames();
		int spacesSize = 1 +synchronizedTableNames.length;
		spaces = new String[spacesSize];
		spaces[0] = tableName;
		if ( ! ArrayHelper.isEmpty( synchronizedTableNames ) ) {
			System.arraycopy( synchronizedTableNames, 0, spaces, 1, spacesSize );
		}

		HashSet<String> subclassTables = new HashSet<String>();
		final EntityBinding[] subEntityBindings = entityBinding.getPreOrderSubEntityBindingClosure();
		/*
		 TODO: here we actually need all entitybindings in the hierarchy, for example:
		 			A
		 		  /   \
		 		 B     C
		 		/      \
		 	  D	        E

		if the current entity is A, then here we need to process all A,B,C,D,E
		but if the current entity D, then here we need D,B,A
		if the current entity is B, then still A,B,D
		 */


		EntityBinding[] ebs = ArrayHelper.join( entityBinding.getEntityBindingClosure(), subEntityBindings );
		for ( EntityBinding subEntityBinding : ebs ) {
			subclassTables.add( subEntityBinding.getPrimaryTable().getQualifiedName( factory.getDialect() ) );
		}
		subclassSpaces = ArrayHelper.toStringArray( subclassTables );

		subquery = generateSubquery( entityBinding );

		if ( isMultiTable() ) {
			int idColumnSpan = getIdentifierColumnSpan();
			ArrayList<String> tableNames = new ArrayList<String>();
			ArrayList<String[]> keyColumns = new ArrayList<String[]>();
			if ( !isAbstract() ) {
				tableNames.add( tableName );
				keyColumns.add( getIdentifierColumnNames() );
			}
			ebs = ArrayHelper.join( new EntityBinding[]{entityBinding}, subEntityBindings );
			for(final EntityBinding eb : ebs){
				TableSpecification tab = eb.getPrimaryTable();
				if ( isNotAbstractUnionTable( eb ) ) {
					String tableName = tab.getQualifiedName( factory.getDialect() );
					tableNames.add( tableName );
					String[] key = new String[idColumnSpan];
					List<org.hibernate.metamodel.spi.relational.Column> columns = tab.getPrimaryKey().getColumns();
					for ( int k = 0; k < idColumnSpan; k++ ) {
						key[k] = columns.get( k ).getColumnName().getText( factory.getDialect() );
					}
					keyColumns.add( key );
				}
			}

			constraintOrderedTableNames = ArrayHelper.toStringArray( tableNames );
			constraintOrderedKeyColumnNames = ArrayHelper.to2DStringArray( keyColumns );
		}
		else {
			constraintOrderedTableNames = new String[] { tableName };
			constraintOrderedKeyColumnNames = new String[][] { getIdentifierColumnNames() };
		}
		initLockers();
		initSubclassPropertyAliasesMap(entityBinding);
		postConstruct(mapping);
	}

	public Serializable[] getQuerySpaces() {
		return subclassSpaces;
	}
	
	public String getTableName() {
		return subquery;
	}

	public Type getDiscriminatorType() {
		return StandardBasicTypes.INTEGER;
	}

	public Object getDiscriminatorValue() {
		return discriminatorValue;
	}

	public String getDiscriminatorSQLValue() {
		return discriminatorSQLValue;
	}

	public String[] getSubclassClosure() {
		return subclassClosure;
	}

	public String getSubclassForDiscriminatorValue(Object value) {
		return (String) subclassByDiscriminatorValue.get(value);
	}

	public Serializable[] getPropertySpaces() {
		return spaces;
	}

	protected boolean isDiscriminatorFormula() {
		return false;
	}

	/**
	 * Generate the SQL that selects a row by id
	 */
	protected String generateSelectString(LockMode lockMode) {
		SimpleSelect select = new SimpleSelect( getFactory().getDialect() )
			.setLockMode(lockMode)
			.setTableName( getTableName() )
			.addColumns( getIdentifierColumnNames() )
			.addColumns( 
					getSubclassColumnClosure(), 
					getSubclassColumnAliasClosure(),
					getSubclassColumnLazyiness()
			)
			.addColumns( 
					getSubclassFormulaClosure(), 
					getSubclassFormulaAliasClosure(),
					getSubclassFormulaLazyiness()
			);
		//TODO: include the rowids!!!!
		if ( hasSubclasses() ) {
			if ( isDiscriminatorFormula() ) {
				select.addColumn( getDiscriminatorFormula(), getDiscriminatorAlias() );
			}
			else {
				select.addColumn( getDiscriminatorColumnName(), getDiscriminatorAlias() );
			}
		}
		if ( getFactory().getSettings().isCommentsEnabled() ) {
			select.setComment( "load " + getEntityName() );
		}
		return select.addCondition( getIdentifierColumnNames(), "=?" ).toStatementString();
	}

	protected String getDiscriminatorFormula() {
		return null;
	}

	protected String getTableName(int j) {
		return tableName;
	}

	protected String[] getKeyColumns(int j) {
		return getIdentifierColumnNames();
	}
	
	protected boolean isTableCascadeDeleteEnabled(int j) {
		return false;
	}
	
	protected boolean isPropertyOfTable(int property, int j) {
		return true;
	}

	// Execute the SQL:

	public String fromTableFragment(String name) {
		return getTableName() + ' '  + name;
	}

	@Override
	public String filterFragment(String name) {
		return hasWhere()
				? " and " + getSQLWhereString( name )
				: "";
	}

	@Override
	protected String filterFragment(String alias, Set<String> treatAsDeclarations) {
		return filterFragment( alias );
	}

	public String getSubclassPropertyTableName(int i) {
		return getTableName();//ie. the subquery! yuck!
	}

	protected void addDiscriminatorToSelect(SelectFragment select, String name, String suffix) {
		select.addColumn( name, getDiscriminatorColumnName(),  getDiscriminatorAlias() );
	}
	
	protected int[] getPropertyTableNumbersInSelect() {
		return new int[ getPropertySpan() ];
	}

	protected int getSubclassPropertyTableNumber(int i) {
		return 0;
	}

	public int getSubclassPropertyTableNumber(String propertyName) {
		return 0;
	}

	public boolean isMultiTable() {
		// This could also just be true all the time...
		return isAbstract() || hasSubclasses();
	}

	public int getTableSpan() {
		return 1;
	}

	protected int[] getSubclassColumnTableNumberClosure() {
		return new int[ getSubclassColumnClosure().length ];
	}

	protected int[] getSubclassFormulaTableNumberClosure() {
		return new int[ getSubclassFormulaClosure().length ];
	}

	protected boolean[] getTableHasColumns() {
		return new boolean[] { true };
	}

	protected int[] getPropertyTableNumbers() {
		return new int[ getPropertySpan() ];
	}

	private boolean isNotAbstractUnionTable(EntityBinding entityBinding) {
//		return !entityBinding.isAbstract() && entityBinding.getHierarchyDetails()
//				.getInheritanceType() != InheritanceType.TABLE_PER_CLASS;
		return !entityBinding.isAbstract();
	}
	private void visitEntityHierarchy(EntityBinding entityBinding, Callback callback){
		EntityBinding rootEntityBinding = entityBinding.getHierarchyDetails().getRootEntityBinding();
		callback.execute( rootEntityBinding );
		visitSubEntityBindings( rootEntityBinding, callback );
	}
	private void visitSubEntityBindings(EntityBinding superEntityBinding, Callback callback){
		callback.execute( superEntityBinding );
		Iterable<EntityBinding> entityBindings= superEntityBinding.getDirectSubEntityBindings();
//		for(EntityBinding entityBinding : entityBindings){
//			callback.execute( entityBinding );
//		}
		for(EntityBinding entityBinding : entityBindings){
			visitSubEntityBindings( entityBinding, callback );
		}
	}
	private static interface Callback{
		void execute(EntityBinding entityBinding);
	}
	protected String generateSubquery(EntityBinding entityBinding){
		final Dialect dialect = getFactory().getDialect();

		if ( !entityBinding.hasSubEntityBindings() ) {
			return entityBinding.getPrimaryTable().getQualifiedName( dialect );
		}

		final HashSet<org.hibernate.metamodel.spi.relational.Column> columns = new LinkedHashSet<org.hibernate.metamodel.spi.relational.Column>();
//		Iterable<EntityBinding> subEntityBindings = entityBinding.getHierarchyDetails()..getEntityBindingClosure();

//		for(EntityBinding eb : subEntityBindings){
//			if ( isNotAbstractUnionTable( eb ) ) {
//				TableSpecification table = entityBinding.getPrimaryTable();
//				for ( Value v : table.values() ) {
//					if ( org.hibernate.metamodel.spi.relational.Column.class.isInstance( v ) ) {
//						columns.add( org.hibernate.metamodel.spi.relational.Column.class.cast( v ) );
//					}
//				}
//			}
//		}

		visitSubEntityBindings(
				entityBinding, new Callback() {
			@Override
			public void execute(EntityBinding eb) {
				if ( isNotAbstractUnionTable( eb ) ) {
					TableSpecification table = eb.getPrimaryTable();
					for ( Value v : table.values() ) {
						if ( org.hibernate.metamodel.spi.relational.Column.class.isInstance( v ) ) {
							columns.add( org.hibernate.metamodel.spi.relational.Column.class.cast( v ) );
						}
					}
				}
			}
		}
		);


		final StringBuilder buf = new StringBuilder()
				.append("( ");

		visitSubEntityBindings( entityBinding, new Callback() {
			@Override
			public void execute(EntityBinding eb) {
				TableSpecification table = eb.getPrimaryTable();
				if ( isNotAbstractUnionTable( eb )) {
					//TODO: move to .sql package!!
					buf.append("select ");
					for(org.hibernate.metamodel.spi.relational.Column column : columns){
						if(!table.hasValue( column )){
							buf.append( dialect.getSelectClauseNullString(column.getJdbcDataType().getTypeCode()) )
									.append(" as ");
						}
						buf.append( column.getColumnName().getText( dialect ) );
						buf.append( ", " );
					}
					buf.append( eb.getSubEntityBindingId() )
							.append( " as clazz_" );
					buf.append(" from ")
							.append( table.getQualifiedName( dialect ));
					buf.append(" union ");
					if ( dialect.supportsUnionAll() ) {
						buf.append("all ");
					}
				}
			}
		} );


		if ( buf.length() > 2 ) {
			//chop the last union (all)
			buf.setLength( buf.length() - ( dialect.supportsUnionAll() ? 11 : 7 ) );
		}

		return buf.append(" )").toString();
	}

	protected String generateSubquery(PersistentClass model, Mapping mapping) {

		Dialect dialect = getFactory().getDialect();
		Settings settings = getFactory().getSettings();
		
		if ( !model.hasSubclasses() ) {
			return model.getTable().getQualifiedName(
					dialect,
					settings.getDefaultCatalogName(),
					settings.getDefaultSchemaName()
				);
		}

		HashSet columns = new LinkedHashSet();
		Iterator titer = model.getSubclassTableClosureIterator();
		while ( titer.hasNext() ) {
			Table table = (Table) titer.next();
			if ( !table.isAbstractUnionTable() ) {
				Iterator citer = table.getColumnIterator();
				while ( citer.hasNext() ) columns.add( citer.next() );
			}
		}

		StringBuilder buf = new StringBuilder()
			.append("( ");

		Iterator siter = new JoinedIterator(
			new SingletonIterator(model),
			model.getSubclassIterator()
		);

		while ( siter.hasNext() ) {
			PersistentClass clazz = (PersistentClass) siter.next();
			Table table = clazz.getTable();
			if ( !table.isAbstractUnionTable() ) {
				//TODO: move to .sql package!!
				buf.append("select ");
				Iterator citer = columns.iterator();
				while ( citer.hasNext() ) {
					Column col = (Column) citer.next();
					if ( !table.containsColumn(col) ) {
						int sqlType = col.getSqlTypeCode(mapping);
						buf.append( dialect.getSelectClauseNullString(sqlType) )
							.append(" as ");
					}
					buf.append( col.getQuotedName(dialect) );
					buf.append(", ");
				}
				buf.append( clazz.getSubclassId() )
					.append(" as clazz_");
				buf.append(" from ")
					.append( table.getQualifiedName(
							dialect,
							settings.getDefaultCatalogName(),
							settings.getDefaultSchemaName()
					) );
				buf.append(" union ");
				if ( dialect.supportsUnionAll() ) {
					buf.append("all ");
				}
			}
		}
		
		if ( buf.length() > 2 ) {
			//chop the last union (all)
			buf.setLength( buf.length() - ( dialect.supportsUnionAll() ? 11 : 7 ) );
		}

		return buf.append(" )").toString();
	}

	protected String[] getSubclassTableKeyColumns(int j) {
		if (j!=0) throw new AssertionFailure("only one table");
		return getIdentifierColumnNames();
	}

	public String getSubclassTableName(int j) {
		if (j!=0) throw new AssertionFailure("only one table");
		return tableName;
	}

	public int getSubclassTableSpan() {
		return 1;
	}

	protected boolean isClassOrSuperclassTable(int j) {
		if (j!=0) throw new AssertionFailure("only one table");
		return true;
	}

	public String getPropertyTableName(String propertyName) {
		//TODO: check this....
		return getTableName();
	}

	public String[] getConstraintOrderedTableNameClosure() {
			return constraintOrderedTableNames;
	}

	public String[][] getContraintOrderedTableKeyColumnClosure() {
		return constraintOrderedKeyColumnNames;
	}

	@Override
	public FilterAliasGenerator getFilterAliasGenerator(String rootAlias) {
		return new StaticFilterAliasGenerator(rootAlias);
	}
}
