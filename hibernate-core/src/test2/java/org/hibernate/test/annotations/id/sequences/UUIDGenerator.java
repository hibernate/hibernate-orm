/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.id.sequences;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.UUID;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

/**
 * Unlike Hibernate's UUID generator.  This avoids 
 * meaningless synchronization and has less
 * than a chance of an asteroid hitting you on the head
 * even afterQuery trillions of rows are inserted.  I know
 * this to be true because it says so in Wikipedia(haha).
 * http://en.wikipedia.org/wiki/UUID#Random_UUID_probability_of_duplicates
 */
public class UUIDGenerator implements IdentifierGenerator {
    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object entity) throws HibernateException {
        UUID uuid = UUID.randomUUID();
        String sud = uuid.toString();
        System.out.println("uuid="+uuid);
        sud = sud.replaceAll("-", "");
        
        BigInteger integer = new BigInteger(sud,16);

        System.out.println("bi ="+integer.toString() );
        return integer;
    }

}
