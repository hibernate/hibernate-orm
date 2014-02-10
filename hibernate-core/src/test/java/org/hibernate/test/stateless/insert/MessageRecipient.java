package org.hibernate.test.stateless.insert;

/**
 * @author mukhanov@gmail.com
 */
public class MessageRecipient {

    private String id;
    private String email;
    private Message message;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }
}
