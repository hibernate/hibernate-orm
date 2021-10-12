package org.hibernate.test.interceptor;

import org.hibernate.EmptyInterceptor;

import java.util.Iterator;

@SuppressWarnings("rawtypes")
public class FlushInterceptor extends EmptyInterceptor {
    @Override
    public void preFlush(Iterator entities) {
        super.preFlush(entities);
        while (entities.hasNext()) {
            Object entity = entities.next();
            if (entity instanceof Team) {
                Team t = (Team) entity;
                System.out.println("Flushed team of " + t.getLeader().getName() + " with PW " + t.leader.getPassword());
            }
        }
    }

    @Override
    public void postFlush(Iterator entities) {
        super.postFlush(entities);
        while (entities.hasNext()) {
            Object entity = entities.next();
            if (entity instanceof Team) {
                Team t = (Team) entity;
                System.out.println("Flushed team with members (PW):");
                for (User member : t.members) {
                    System.out.println(member.getName() + " (" + member.getPassword() + ")");
                }
            }
        }
    }
}
