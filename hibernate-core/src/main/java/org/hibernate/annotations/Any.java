/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.annotations;
import java.lang.annotation.Retention;
import javax.persistence.Column;
import javax.persistence.FetchType;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Defines a ToOne-style association pointing to one of several entity types depending on a local discriminator,
 * as opposed to discriminated inheritance where the discriminator is kept as part of the entity hierarchy.
 *
 * For example, if you consider an Order entity containing Payment information where Payment might be of type
 * CashPayment or CreditCardPayment the @Any approach would be to keep that discriminator and matching value on the
 * Order itself.  Thought of another way, the "foreign-key" really is made up of the value and discriminator
 * (there is no physical foreign key here as databases do not support this):
 * <blockquote><pre>
 *    &#064;Entity
 *    class Order {
 *        ...
 *        &#064;Any( metaColumn = @Column( name="payment_type" ) )
 *        &#064;AnyMetDef(
 *                idType = "long"
 *                metaValues = {
 *                        &#064;MetaValue( value="C", targetEntity=CashPayment.class ),
 *                        &#064;MetaValue( value="CC", targetEntity=CreditCardPayment.class ),
 *                }
 *        )
 *        pubic Payment getPayment() { ... }
 *    }
 * }
 * </pre></blockquote>
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 *
 * @see AnyMetaDef
 */
@java.lang.annotation.Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface Any {
	/**
	 * Metadata definition used.
	 * If defined, should point to a @AnyMetaDef name
	 * If not defined, the local (ie in the same field or property) @AnyMetaDef is used
	 */
	String metaDef() default "";

	/**
	 * Identifies the discriminator column.  This column will hold the value that identifies the targeted entity.
	 */
	Column metaColumn();

	/**
	 * Defines whether the value of the field or property should be lazily loaded or must be
	 * eagerly fetched. The EAGER strategy is a requirement on the persistence provider runtime
	 * that the value must be eagerly fetched. The LAZY strategy is applied when bytecode
	 * enhancement is used. If not specified, defaults to EAGER.
	 */
	FetchType fetch() default FetchType.EAGER;
	/**
	 * Whether the association is optional. If set to false then a non-null relationship must always exist.
	 */
	boolean optional() default true;
}
