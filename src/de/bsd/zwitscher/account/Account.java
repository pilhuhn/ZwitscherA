package de.bsd.zwitscher.account;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * One server account
 *
 * @author Heiko W. Rupp
 */
public class Account implements Parcelable {
    private final int id;
    private final String name;
    private String accessTokenKey;
    private String accessTokenSecret;
    private final Type serverType;
    private String serverUrl;
    private final boolean defaultAccount;
    private String password;

    public Account(int id, String name, String accessTokenKey, String accessTokenSecret, String serverUrl, Type serverType,boolean defaultAccount) {
        this.id = id;
        this.name = name;
        this.accessTokenKey = accessTokenKey;
        this.accessTokenSecret = accessTokenSecret;
        this.serverType = serverType;
        this.serverUrl = serverUrl;
        this.defaultAccount = defaultAccount;
    }

    public Account(int id, String name, String serverUrl, Type serverType, boolean defaultAccount, String password) {
        this.id = id;
        this.name = name;
        this.serverUrl = serverUrl;
        this.serverType = serverType;
        this.defaultAccount = defaultAccount;
        this.password = password;
    }

    public Account(Parcel parcel) {
        id = parcel.readInt();
        name = parcel.readString();
        accessTokenKey = parcel.readString();
        accessTokenSecret = parcel.readString();
        serverType = Type.valueOf(parcel.readString());
        serverUrl  = parcel.readString();
        defaultAccount = parcel.readInt() == 1;
        password = parcel.readString();

    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAccessTokenKey() {
        return accessTokenKey;
    }

    public void setAccessTokenKey(String accessTokenKey) {
        this.accessTokenKey = accessTokenKey;
    }

    public String getAccessTokenSecret() {
        return accessTokenSecret;
    }

    public void setAccessTokenSecret(String accessTokenSecret) {
        this.accessTokenSecret = accessTokenSecret;
    }

    public Type getServerType() {
        return serverType;
    }

    public String getServerUrl() {

        if (serverType==Type.IDENTICA)
            return "https://identi.ca/";

        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public boolean isDefaultAccount() { return defaultAccount; }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isStatusNet() {
        return serverType!=Type.TWITTER;
    }

    /**
     * Return a canonical representation of this account that can be used
     * within various places in the UI
     * @return Human readable identifier of this account
     */
    public String getAccountIdentifier() {
        if (serverUrl!=null && !(serverUrl.length() == 0)) {
            String tmp = serverUrl;
            if (tmp.startsWith("http") && tmp.contains("://"))
                tmp = tmp.substring(tmp.indexOf(":")+3);
            if (tmp.endsWith("/"))
                tmp = tmp.substring(0,tmp.length()-1);
            return name + "@" + tmp;
        }
        else {
            return name + "@" + serverType.getServerTypeName();
        }
    }

    public int describeContents() {
        return 0;  // TODO: Customise this generated block
    }

    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeString(name);
        parcel.writeString(accessTokenKey);
        parcel.writeString(accessTokenSecret);
        parcel.writeString(serverType.name());
        parcel.writeString(serverUrl);
        parcel.writeInt(defaultAccount ? 1 : 0);
        parcel.writeString(password);
    }

    public static Creator<Account> CREATOR = new Creator<Account>() {
        public Account createFromParcel(Parcel parcel) {
            return new Account(parcel);
        }

        public Account[] newArray(int size) {
            return new Account[size];
        }
    };

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Account account = (Account) o;

        return id == account.id;

    }

    public int hashCode() {
        return id;
    }

    /**
     * Return account details. For a human readable representation
     * see {@link #getAccountIdentifier}
     * @return String describing the account.
     */
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Account");
        sb.append("{id=").append(id);
        sb.append(", name='").append(name).append('\'');
        sb.append(", serverType='").append(serverType).append('\'');
        sb.append(", serverUrl='").append(serverUrl).append('\'');
        sb.append(", defaultAccount=").append(defaultAccount);
        sb.append(", accessToken='").append(accessTokenSecret != null ? "-set-" : "-unset-").append('\'');
        sb.append(", password='").append(password!=null?"-set-":"-unset-").append('\'');
        sb.append('}');
        return sb.toString();
    }

    public String getStatusType() {
        if (isStatusNet())
            return "Dent";
        else
            return "Tweet";
    }

    public enum Type {
        TWITTER("Twitter","Tweet"),
        IDENTICA("Identi.ca","Dent"),
        STATUSNET("Status.net","Dent")
        ;
        private final String serverTypeName;
        private final String statusName;

        private Type(String serverTypeName, String statusName) {
            this.serverTypeName = serverTypeName;
            this.statusName = statusName;
        }

        public String getServerTypeName() {
            return serverTypeName;
        }

        public String getStatusName() {
            return statusName;
        }
    }
}
