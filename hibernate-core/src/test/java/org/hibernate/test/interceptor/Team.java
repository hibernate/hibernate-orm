package org.hibernate.test.interceptor;

import java.util.List;
import java.util.Set;

public class Team {
    String name;
    User leader;
    Set<User> members;

    public Team(String name, User leader, Set<User> members) {
        super();
        this.name = name;
        this.leader = leader;
        this.members = members;
    }

    public Team() { super(); }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public User getLeader() {
        return leader;
    }

    public void setLeader(User leader) {
        this.leader = leader;
    }

    public Set<User> getMembers() {
        return members;
    }

    public void setMembers(Set<User> members) {
        this.members = members;
    }
}
