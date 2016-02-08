package org.hibernate.userguide.criteria;

/**
 * @author Vlad Mihalcea
 */
//tag::criteria-typedquery-wrapper-example[]
public class PersonWrapper {

    private final Long id;

    private final String nickName;

    public PersonWrapper(Long id, String nickName) {
        this.id = id;
        this.nickName = nickName;
    }

    public Long getId() {
        return id;
    }

    public String getNickName() {
        return nickName;
    }
}
//end::criteria-typedquery-wrapper-example[]

