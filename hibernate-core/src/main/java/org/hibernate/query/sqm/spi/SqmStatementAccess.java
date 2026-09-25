package org.hibernate.query.sqm.spi;

import org.hibernate.query.sqm.tree.spi.SqmStatement;

/// Provides access to an underlying SQM AST
///
/// @author Steve Ebersole
public interface SqmStatementAccess<R> {
	/// Return the SQM AST.
	SqmStatement<R> getSqmStatement();
}
