package org.cis120;

import java.util.*;

public final class Channel implements Comparable {

    private Collection<String> userList;
    private String owner;
    private boolean isPrivate;

    public Channel(String tempOwner, boolean tempIsPrivate) {
        this.isPrivate = tempIsPrivate;
        this.owner = tempOwner;
        userList = new TreeSet<String>();
        userList.add(owner);
    }

    public boolean getIsPrivate() {
        return isPrivate;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String tempNewOwner) {
        this.owner = tempNewOwner;
    }

    public Collection<String> getUserList() {
        return userList;
    }

    public void addUser(String nickname) {
        userList.add(nickname);
    }

    public void banUser(String nickname) {
        if (userList.contains(nickname)) {
            userList.remove(nickname);
        }
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }
}