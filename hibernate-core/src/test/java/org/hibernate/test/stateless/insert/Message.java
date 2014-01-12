package org.hibernate.test.stateless.insert;

/**
 * @author mukhanov@gmail.com
 */
public class Message {

    private String id;
    private String subject;
    private String content;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
