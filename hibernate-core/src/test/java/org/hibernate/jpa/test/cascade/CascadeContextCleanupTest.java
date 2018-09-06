package org.hibernate.jpa.test.cascade;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.junit.Test;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class CascadeContextCleanupTest extends BaseEntityManagerFunctionalTestCase {

  @Override
  protected Class<?>[] getAnnotatedClasses() {
    return new Class<?>[] {
            TableEntity.class,
            ColumnEntity.class,
            RelationEntity.class
    };
  }

  @Test
  public void testCascadeContextCleanup() {

    final EntityManager entityManager = getOrCreateEntityManager();

    // create test data: Table1 -> Column -> Relation -> Table2
    createTablesData( entityManager );

    // remove table 1
    // expected to remove Table, Column and Relation
    // and it does remove them from DB, but they stay in Persistence Context
    entityManager.getTransaction().begin();
    final TableEntity table = entityManager.find( TableEntity.class, "1" );
    assertEquals(1, table.getColumns().size());
    entityManager.remove( table );
    entityManager.getTransaction().commit();

    // remove table 2
    // throws org.hibernate.TransientPropertyValueException
    entityManager.getTransaction().begin();
    final TableEntity table2 = entityManager.find( TableEntity.class, "2" );
    entityManager.remove( table2 );
    entityManager.getTransaction().commit();

    entityManager.close();
  }

  private void createTablesData( EntityManager entityManager ) {

    entityManager.getTransaction().begin();

    final TableEntity parentTable = new TableEntity();
    parentTable.setId( "1" );
    entityManager.persist( parentTable );

    final TableEntity childTable = new TableEntity();
    childTable.setId( "2" );
    entityManager.persist( childTable );

    final ColumnEntity column = new ColumnEntity();
    column.setId( "1" );
    column.setTable( parentTable );
    parentTable.getColumns().add( column );
    entityManager.persist( column );

    final RelationEntity relation = new RelationEntity();
    relation.setForeignTable( parentTable );
    parentTable.getRelationsToThisTable().add( relation );
    relation.setColumn( column );
    column.setRelationEntity( relation );
    entityManager.persist( relation );

    entityManager.getTransaction().commit();
  }

  @Entity
  public static class TableEntity {

    private String id;
    private Collection<ColumnEntity> columns = new ArrayList<>();
    private Collection<RelationEntity> relationsToThisTable = new ArrayList<>();

    @Id
    public String getId() {
      return id;
    }

    public void setId( String id ) {
      this.id = id;
    }

    @OneToMany( mappedBy = "table", cascade = CascadeType.ALL, orphanRemoval = true )
    //@OnDelete( action = OnDeleteAction.CASCADE )
    public Collection<ColumnEntity> getColumns() {
      return columns;
    }

    public void setColumns( Collection<ColumnEntity> columns ) {
      this.columns = columns;
    }

    @OneToMany( mappedBy = "foreignTable", cascade = CascadeType.ALL, orphanRemoval = true )
    //@OnDelete( action = OnDeleteAction.CASCADE )
    public Collection<RelationEntity> getRelationsToThisTable() {
      return relationsToThisTable;
    }

    public void setRelationsToThisTable( Collection<RelationEntity> relationsToThisTable ) {
      this.relationsToThisTable = relationsToThisTable;
    }
  }

  @Entity
  public static class ColumnEntity {

    private String id;
    private String tableId;

    private TableEntity table;
    private RelationEntity relationEntity;

    @Id
    public String getId()
    {
      return id;
    }

    public void setId( String id ) {
      this.id = id;
    }

    @Column( name = "tableId", insertable = false, updatable = false )
    public String getTableId() {
      return tableId;
    }

    private void setTableId( String tableId ) {
      this.tableId = tableId;
    }

    @ManyToOne( fetch = FetchType.LAZY )
    @JoinColumn( name = "tableId" )
    public TableEntity getTable() {
      return table;
    }

    public void setTable( TableEntity table ) {
      this.setTableId( table.getId() );
      this.table = table;
    }

    @OneToOne(mappedBy = "column", cascade = CascadeType.ALL)
    public RelationEntity getRelationEntity() {
      return relationEntity;
    }

    public void setRelationEntity(RelationEntity relationEntity) {
      this.relationEntity = relationEntity;
    }
  }

  @Entity
  public static class RelationEntity {

    private String foreignTableId;
    private String columnId;

    private TableEntity foreignTable;
    private ColumnEntity column;

    @Id
    public String getColumnId() {
      return columnId;
    }

    private void setColumnId( String columnId ) {
      this.columnId = columnId;
    }

    @Column( name = "foreignTableId", nullable = false, insertable = false, updatable = false )
    public String getForeignTableId() {
      return foreignTableId;
    }

    private void setForeignTableId( String foreignTableId ) {
      this.foreignTableId = foreignTableId;
    }

    @ManyToOne
    @JoinColumn( name = "foreignTableId", referencedColumnName = "id", nullable = false )
    public TableEntity getForeignTable() {
      return foreignTable;
    }

    public void setForeignTable( TableEntity foreignTable ) {
      this.setForeignTableId( foreignTable.getId() );
      this.foreignTable = foreignTable;
    }

    @OneToOne( fetch = FetchType.LAZY )
    @MapsId
    @JoinColumn( name = "columnId" )
    //@OnDelete( action = OnDeleteAction.CASCADE )
    public ColumnEntity getColumn() {
      return column;
    }

    public void setColumn( ColumnEntity column ) {
      this.setColumnId( column.getId() );
      this.column = column;
    }
  }
}
