/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.collectionbasictype;

import java.util.List;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

/**
 * @author Vlad Mihalcea
 */
public class CommaDelimitedStringsType extends AbstractSingleColumnStandardBasicType<List> {

    public CommaDelimitedStringsType() {
        super(
            VarcharTypeDescriptor.INSTANCE,
            new CommaDelimitedStringsJavaTypeDescriptor()
        );
    }

    @Override
    public String getName() {
        return "comma_delimited_strings";
    }
}