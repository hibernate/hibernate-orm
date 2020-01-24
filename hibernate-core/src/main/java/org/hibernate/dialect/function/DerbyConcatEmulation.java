/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.query.sqm.function.VarArgsFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.type.StandardBasicTypes;

/**
 * A specialized concat() function definition in which:<ol>
 * <li>we translate to use the concat operator ('||')</li>
 * <li>wrap dynamic parameters in CASTs to VARCHAR</li>
 * </ol>
 * <p/>
 * This last spec is to deal with a limitation on DB2 and variants (e.g. Derby)
 * where dynamic parameters cannot be used in concatenation unless they are being
 * concatenated with at least one non-dynamic operand.  And even then, the rules
 * are so convoluted as to what is allowed and when the CAST is needed and when
 * it is not that we just go ahead and do the CASTing.
 *
 * @author Steve Ebersole
 * @author Christian Beikov
 */
public class DerbyConcatEmulation extends VarArgsFunctionDescriptor {
	public DerbyConcatEmulation() {
		super(
				"concat",
				"(",
				"||",
				")",
				StandardArgumentsValidators.min( 2 ),
				StandardFunctionReturnTypeResolvers.invariant( StandardBasicTypes.STRING )
		);
	}
}
