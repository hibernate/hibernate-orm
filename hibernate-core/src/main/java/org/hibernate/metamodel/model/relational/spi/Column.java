/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.relational.spi;

import java.util.Comparator;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.QualifiableSqlExpressable;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Represents the commonality between {@link PhysicalColumn} and {@link DerivedColumn}
 *
 * @author Steve Ebersole
 */
public interface Column extends QualifiableSqlExpressable {
	Table getSourceTable();

	/**
	 * A simple "loggable" String representation of the Column
	 */
	String getExpression();

	Size getSize();

	// todo : nullable, etc

	// todo : org.hibernate.annotations.ColumnTransformer#read and org.hibernate.annotations.ColumnTransformer#write?

	String toLoggableString();

	// todo (6.0) : need to account for rendering a column for both read and write separately
	//		this is to help account for custom read/write fragments on PhysicalColumn.  We
	//		can apply the same logic (polymorphism) to DerivedColumns as well, except that:
	//			1) the DerivedColumn's reader is always the formula expression (illegal to apply transformer)
	//			2) the DerivedColumn's writer is always null. again illegal to apply transformer, and by definition a DerivedColumn is read-only
	String render(String identificationVariable);

	String render();

	default String renderReadExpression(String identificationVariable) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	default String renderWriteExpression(String identificationVariable) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	SqlTypeDescriptor getSqlTypeDescriptor();

	BasicJavaDescriptor getJavaTypeDescriptor();

	@Override
	default Expression createSqlExpression(
			ColumnReferenceQualifier qualifier,
			SqlAstCreationState creationState) {
		return new ColumnReference( qualifier, this );
	}

	Comparator<Column> COLUMN_COMPARATOR = Comparator.comparing( Column::getExpression );

	boolean isInsertable();

	boolean isUpdatable();

	// todo (6.0) : a better name for the method ?
	/**
	 * Create a clone of the Column setting the insertability and updability values
	 * based on the passed parameters
	 *
	 * @param isInsertable, the value to set for the cloned Column insertability
	 * @param isUpdatable, the value to set for the cloned Column updatability
	 *
	 * @return A clone of the object
	 */
	Column clone(boolean isInsertable, boolean isUpdatable);
}
